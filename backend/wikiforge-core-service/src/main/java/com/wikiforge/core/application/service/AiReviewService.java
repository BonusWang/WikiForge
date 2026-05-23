package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.core.application.dto.AiReviewRunResponse;
import com.wikiforge.core.application.dto.CreateAiReviewRunRequest;
import com.wikiforge.core.application.dto.ReviewItemPageResponse;
import com.wikiforge.core.application.dto.ReviewItemResponse;
import com.wikiforge.core.domain.model.AgentRun;
import com.wikiforge.core.domain.model.AgentStep;
import com.wikiforge.core.domain.model.ReviewItem;
import com.wikiforge.core.domain.model.ReviewItemPage;
import com.wikiforge.core.domain.model.SourceContent;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.repository.AgentReviewRepository;
import com.wikiforge.core.domain.repository.SourceContentRepository;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class AiReviewService {

    private static final String PIPELINE_VERSION = "mvp4-ai-review-v1";
    private static final String PROMPT_VERSION = "ai-review-draft-v1";
    private static final int MAX_MODEL_INPUT_CHARS = 8000;
    private static final int MAX_EXCERPT_CHARS = 500;

    private final SourceFileRepository sourceFileRepository;
    private final SourceContentRepository sourceContentRepository;
    private final AgentReviewRepository agentReviewRepository;
    private final CoreRuntimeProperties runtimeProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AiReviewService(
            SourceFileRepository sourceFileRepository,
            SourceContentRepository sourceContentRepository,
            AgentReviewRepository agentReviewRepository,
            CoreRuntimeProperties runtimeProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.sourceFileRepository = sourceFileRepository;
        this.sourceContentRepository = sourceContentRepository;
        this.agentReviewRepository = agentReviewRepository;
        this.runtimeProperties = runtimeProperties;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiReviewRunResponse createRun(String fileUid, CreateAiReviewRunRequest request) {
        SourceFileRecord sourceFile = sourceFileRepository.findByFileUid(fileUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND));
        SourceContent sourceContent = sourceContentRepository.findBySourceFileUid(fileUid).orElse(null);
        AiProviderConfig modelSelection = selectModel(request);
        AiReviewDraft draft = generateDraft(sourceFile, sourceContent, request, modelSelection);

        LocalDateTime now = LocalDateTime.now();
        AgentRun run = agentReviewRepository.saveRun(new AgentRun(
                null,
                newUid("run"),
                sourceFile.sourceId(),
                sourceFile.sourceFileId(),
                "ai_review",
                PIPELINE_VERSION,
                "completed",
                "draft_review",
                modelSelection.providerName(),
                modelSelection.modelName(),
                now,
                now,
                "needs_human_review",
                null,
                now,
                now
        ));

        agentReviewRepository.saveStep(new AgentStep(
                null,
                newUid("step"),
                run.id(),
                sourceFile.sourceId(),
                sourceFile.sourceFileId(),
                "draft_review",
                "WikiForge Organizer",
                "completed",
                stepInputJson(sourceFile, modelSelection),
                draft.suggestedChangesJson(),
                modelSelection.providerName(),
                modelSelection.modelName(),
                PROMPT_VERSION,
                draft.providerNotice(),
                now,
                now,
                now
        ));

        ReviewItem reviewItem = agentReviewRepository.saveReviewItem(new ReviewItem(
                null,
                newUid("review"),
                sourceFile.sourceId(),
                sourceFile.sourceFileId(),
                run.id(),
                "ai整理建议",
                "pending",
                draft.reason(),
                draft.suggestedChangesJson(),
                draft.markdownDraft(),
                null,
                null,
                now,
                now,
                null,
                null,
                null
        ));

        return toRunResponse(run, reviewItem);
    }

    public AiReviewRunResponse getRun(String runUid) {
        AgentRun run = agentReviewRepository.findRunByRunUid(runUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_REVIEW_RUN_NOT_FOUND));
        ReviewItem reviewItem = agentReviewRepository.findLatestReviewItemByRunId(run.id()).orElse(null);
        return toRunResponse(run, reviewItem);
    }

    public ReviewItemPageResponse listReviewItems(String status, int page, int pageSize) {
        ReviewItemPage reviewItemPage = agentReviewRepository.findReviewItems(status, page, pageSize);
        return new ReviewItemPageResponse(
                reviewItemPage.items().stream().map(this::toReviewItemResponse).toList(),
                reviewItemPage.page(),
                reviewItemPage.pageSize(),
                reviewItemPage.total()
        );
    }

    private AiReviewDraft generateDraft(
            SourceFileRecord sourceFile,
            SourceContent sourceContent,
            CreateAiReviewRunRequest request,
            AiProviderConfig modelSelection
    ) {
        if (modelSelection.ruleBased()) {
            return generateRuleBasedDraft(sourceFile, sourceContent, null);
        }
        if (!modelSelection.openAiCompatible()) {
            return generateRuleBasedDraft(
                    sourceFile,
                    sourceContent,
                    displayProviderName(modelSelection.providerName()) + " 暂不支持 providerType="
                            + modelSelection.providerType() + "，已使用本地规则兜底。"
            );
        }
        if (!modelSelection.readyForRemoteCall()) {
            return generateRuleBasedDraft(sourceFile, sourceContent, missingConfigNotice(modelSelection));
        }
        try {
            return generateOpenAiCompatibleDraft(sourceFile, sourceContent, request, modelSelection);
        } catch (RuntimeException exception) {
            return generateRuleBasedDraft(
                    sourceFile,
                    sourceContent,
                    displayProviderName(modelSelection.providerName()) + " 调用失败，已使用本地规则兜底：" + safeMessage(exception)
            );
        }
    }

    private AiReviewDraft generateOpenAiCompatibleDraft(
            SourceFileRecord sourceFile,
            SourceContent sourceContent,
            CreateAiReviewRunRequest request,
            AiProviderConfig modelSelection
    ) {
        String rawText = rawText(sourceContent);
        String prompt = """
                请作为 WikiForge 的知识整理 Agent，基于给定源文件正文生成审核草案。
                输出严格 JSON，不要 Markdown 代码块。字段：
                summary: 中文摘要，tags: 中文标签数组，category: 推荐归档目录，
                risks: 风险提醒数组，markdownDraft: 可写入 Obsidian 的 Markdown 草案。
                文件名：%s
                正文：
                %s
                """.formatted(sourceFile.fileName(), truncate(rawText, MAX_MODEL_INPUT_CHARS));

        Map<String, Object> body = Map.of(
                "model", modelSelection.modelName(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是 WikiForge 知识熔炉的资料整理员，输出必须稳定、可审核、可追溯。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        JsonNode response = restClientBuilder
                .baseUrl(trimTrailingSlash(modelSelection.baseUrl()))
                .build()
                .post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + modelSelection.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String content = response == null
                ? ""
                : response.path("choices").path(0).path("message").path("content").asText("");
        JsonNode draftJson = parseJsonObject(content);
        if (draftJson == null) {
            return draftFromSummary(
                    sourceFile,
                    sourceContent,
                    content,
                    displayProviderName(modelSelection.providerName()) + " 返回非 JSON 内容，已转成审核草案。"
            );
        }
        return draftFromJson(sourceFile, sourceContent, draftJson, null, modelSelection);
    }

    private AiReviewDraft generateRuleBasedDraft(
            SourceFileRecord sourceFile,
            SourceContent sourceContent,
            String providerNotice
    ) {
        String rawText = rawText(sourceContent);
        String summary = firstSentence(rawText);
        if (!hasText(summary)) {
            summary = "源文件暂无可解析正文，需要人工补充整理。";
        }
        ObjectNode suggested = objectMapper.createObjectNode();
        suggested.put("summary", summary);
        suggested.put("category", "01_Inbox/待整理");
        ArrayNode tags = suggested.putArray("tags");
        tags.add("知识管理");
        tags.add("待整理");
        if (hasText(sourceFile.fileExt())) {
            tags.add(sourceFile.fileExt().toLowerCase(Locale.ROOT));
        }
        ArrayNode risks = suggested.putArray("risks");
        risks.add("AI 整理结果需人工审核后再写入 Obsidian。");
        suggested.put("sourceExcerpt", truncate(rawText, MAX_EXCERPT_CHARS));
        if (hasText(providerNotice)) {
            suggested.put("providerNotice", providerNotice);
        }
        String markdownDraft = """
                ## AI 整理建议

                ### 摘要
                %s

                ### 推荐归档
                - 目录：01_Inbox/待整理
                - 标签：知识管理、待整理

                ### 正文摘录
                %s
                """.formatted(summary, truncate(rawText, MAX_EXCERPT_CHARS));
        return new AiReviewDraft(
                toJson(suggested),
                markdownDraft,
                "基于已解析正文生成待审核整理建议。",
                providerNotice
        );
    }

    private AiReviewDraft draftFromJson(
            SourceFileRecord sourceFile,
            SourceContent sourceContent,
            JsonNode draftJson,
            String providerNotice,
            AiProviderConfig modelSelection
    ) {
        ObjectNode suggested = objectMapper.createObjectNode();
        suggested.put("summary", draftJson.path("summary").asText(firstSentence(rawText(sourceContent))));
        suggested.set("tags", draftJson.path("tags").isArray() ? draftJson.path("tags") : defaultTags(sourceFile));
        suggested.put("category", draftJson.path("category").asText("01_Inbox/待整理"));
        suggested.set("risks", draftJson.path("risks").isArray() ? draftJson.path("risks") : defaultRisks());
        suggested.put("sourceExcerpt", truncate(rawText(sourceContent), MAX_EXCERPT_CHARS));
        if (hasText(providerNotice)) {
            suggested.put("providerNotice", providerNotice);
        }
        String markdownDraft = draftJson.path("markdownDraft").asText("");
        if (!hasText(markdownDraft)) {
            markdownDraft = markdownDraftFromSuggested(suggested);
        }
        return new AiReviewDraft(
                toJson(suggested),
                markdownDraft,
                displayProviderName(modelSelection.providerName()) + " 已生成结构化整理建议，等待人工审核。",
                providerNotice
        );
    }

    private AiReviewDraft draftFromSummary(
            SourceFileRecord sourceFile,
            SourceContent sourceContent,
            String summary,
            String providerNotice
    ) {
        ObjectNode suggested = objectMapper.createObjectNode();
        suggested.put("summary", hasText(summary) ? summary : firstSentence(rawText(sourceContent)));
        suggested.set("tags", defaultTags(sourceFile));
        suggested.put("category", "01_Inbox/待整理");
        suggested.set("risks", defaultRisks());
        suggested.put("sourceExcerpt", truncate(rawText(sourceContent), MAX_EXCERPT_CHARS));
        suggested.put("providerNotice", providerNotice);
        return new AiReviewDraft(
                toJson(suggested),
                markdownDraftFromSuggested(suggested),
                "模型已返回内容，系统转成待审核整理建议。",
                providerNotice
        );
    }

    private AiProviderConfig selectModel(CreateAiReviewRunRequest request) {
        return runtimeProperties.aiProviderConfig(
                request == null ? null : request.providerName(),
                request == null ? null : request.providerType(),
                request == null ? null : request.baseUrl(),
                request == null ? null : request.modelName()
        );
    }

    private AiReviewRunResponse toRunResponse(AgentRun run, ReviewItem reviewItem) {
        return new AiReviewRunResponse(
                run.runUid(),
                reviewItem == null ? null : reviewItem.sourceUid(),
                reviewItem == null ? null : reviewItem.sourceFileUid(),
                run.status(),
                run.currentStep(),
                run.modelProvider(),
                run.modelName(),
                reviewItem == null ? null : reviewItem.reviewUid(),
                reviewItem == null ? null : reviewItem.status(),
                toOffset(run.createdAt())
        );
    }

    private ReviewItemResponse toReviewItemResponse(ReviewItem reviewItem) {
        return new ReviewItemResponse(
                reviewItem.reviewUid(),
                reviewItem.sourceUid(),
                reviewItem.sourceFileUid(),
                reviewItem.runUid(),
                reviewItem.reviewType(),
                reviewItem.status(),
                reviewItem.reason(),
                reviewItem.suggestedChangesJson(),
                reviewItem.markdownDraft(),
                toOffset(reviewItem.createdAt())
        );
    }

    private JsonNode parseJsonObject(String content) {
        if (!hasText(content)) {
            return null;
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            int firstLineEnd = json.indexOf('\n');
            int fenceEnd = json.lastIndexOf("```");
            if (firstLineEnd >= 0 && fenceEnd > firstLineEnd) {
                json = json.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }
        int objectStart = json.indexOf('{');
        int objectEnd = json.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            json = json.substring(objectStart, objectEnd + 1);
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject() ? node : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private ArrayNode defaultTags(SourceFileRecord sourceFile) {
        ArrayNode tags = objectMapper.createArrayNode();
        tags.add("知识管理");
        tags.add("待整理");
        if (hasText(sourceFile.fileExt())) {
            tags.add(sourceFile.fileExt().toLowerCase(Locale.ROOT));
        }
        return tags;
    }

    private ArrayNode defaultRisks() {
        ArrayNode risks = objectMapper.createArrayNode();
        risks.add("AI 整理结果需人工审核后再写入 Obsidian。");
        return risks;
    }

    private String markdownDraftFromSuggested(JsonNode suggested) {
        return """
                ## AI 整理建议

                ### 摘要
                %s

                ### 推荐归档
                - 目录：%s
                """.formatted(
                suggested.path("summary").asText("待人工补充摘要。"),
                suggested.path("category").asText("01_Inbox/待整理")
        );
    }

    private String stepInputJson(SourceFileRecord sourceFile, AiProviderConfig modelSelection) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("sourceUid", sourceFile.sourceUid());
        input.put("sourceFileUid", sourceFile.fileUid());
        input.put("providerName", modelSelection.providerName());
        input.put("providerType", modelSelection.providerType());
        input.put("modelName", modelSelection.modelName());
        input.put("baseUrlConfigured", hasText(modelSelection.baseUrl()));
        input.put("pipelineVersion", PIPELINE_VERSION);
        return toJson(input);
    }

    private String rawText(SourceContent sourceContent) {
        return sourceContent == null ? "" : nullToEmpty(sourceContent.rawText());
    }

    private String firstSentence(String rawText) {
        String normalized = nullToEmpty(rawText).replace("\r", " ").replace("\n", " ").trim();
        if (!hasText(normalized)) {
            return "";
        }
        int chinesePeriod = normalized.indexOf('。');
        int englishPeriod = normalized.indexOf('.');
        int end = -1;
        if (chinesePeriod >= 0 && englishPeriod >= 0) {
            end = Math.min(chinesePeriod, englishPeriod);
        } else if (chinesePeriod >= 0) {
            end = chinesePeriod;
        } else if (englishPeriod >= 0) {
            end = englishPeriod;
        }
        if (end >= 0) {
            return normalized.substring(0, end + 1);
        }
        return truncate(normalized, 160);
    }

    private String truncate(String value, int maxLength) {
        String normalized = nullToEmpty(value).trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "json cannot be generated");
        }
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String newUid(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + date + "_" + randomSuffix;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String missingConfigNotice(AiProviderConfig modelSelection) {
        String providerName = displayProviderName(modelSelection.providerName());
        if (isMiniMax(modelSelection.providerName())) {
            return providerName + " 未配置密钥或模型，已使用本地规则兜底。";
        }
        return providerName + " 未配置密钥、Base URL 或模型，已使用本地规则兜底。";
    }

    private String displayProviderName(String providerName) {
        if (isMiniMax(providerName)) {
            return "MiniMax";
        }
        return firstText(providerName, "模型");
    }

    private boolean isMiniMax(String providerName) {
        if (!hasText(providerName)) {
            return false;
        }
        String normalized = providerName.toLowerCase(Locale.ROOT);
        return normalized.contains("minimax") || normalized.contains("minmax");
    }

    private String trimTrailingSlash(String value) {
        String result = firstText(value, "https://api.minimax.io/v1");
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (!hasText(message)) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record AiReviewDraft(
            String suggestedChangesJson,
            String markdownDraft,
            String reason,
            String providerNotice
    ) {
    }
}
