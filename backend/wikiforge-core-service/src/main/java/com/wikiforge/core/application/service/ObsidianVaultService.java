package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.ObsidianInitResponse;
import com.wikiforge.core.application.dto.ObsidianNotePreviewResponse;
import com.wikiforge.core.application.dto.ObsidianNoteResponse;
import com.wikiforge.core.application.dto.ObsidianVaultStatusResponse;
import com.wikiforge.core.application.dto.SourceNoteDraftResponse;
import com.wikiforge.core.domain.model.ObsidianNote;
import com.wikiforge.core.domain.model.SourceContent;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.repository.ObsidianNoteRepository;
import com.wikiforge.core.domain.repository.SourceContentRepository;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObsidianVaultService {

    private static final String SOURCE_NOTE_TYPE = "source_note";
    private static final String WIKI_SOURCE_PAGE_TYPE = "wiki_source_page";
    private static final String WRITTEN_STATUS = "written";
    private static final String MANAGED_ROOT = "WikiForge";
    private static final String WIKI_SOURCE_DIRECTORY = MANAGED_ROOT + "/10_来源";
    private static final String WIKI_INDEX_PATH = MANAGED_ROOT + "/index.md";
    private static final String WIKI_LOG_PATH = MANAGED_ROOT + "/log.md";
    private static final String SOURCE_NOTE_DIRECTORY = "00_Inbox_收集箱/Sources_来源";
    private static final List<String> VAULT_DIRECTORIES = List.of(
            MANAGED_ROOT,
            MANAGED_ROOT + "/00_规则",
            WIKI_SOURCE_DIRECTORY,
            MANAGED_ROOT + "/20_主题",
            MANAGED_ROOT + "/30_项目",
            MANAGED_ROOT + "/90_系统",
            "00_Inbox_收集箱",
            SOURCE_NOTE_DIRECTORY,
            "00_Inbox_收集箱/Personal_个人记录",
            "10_Wiki_主题库",
            "20_Projects_项目",
            "30_Resources_资源",
            "90_System_系统"
    );

    private final SourceFileRepository sourceFileRepository;
    private final SourceContentRepository sourceContentRepository;
    private final ObsidianNoteRepository obsidianNoteRepository;
    private final CoreRuntimeProperties runtimeProperties;
    private final ObjectMapper objectMapper;

    public ObsidianVaultService(
            SourceFileRepository sourceFileRepository,
            SourceContentRepository sourceContentRepository,
            ObsidianNoteRepository obsidianNoteRepository,
            CoreRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        this.sourceFileRepository = sourceFileRepository;
        this.sourceContentRepository = sourceContentRepository;
        this.obsidianNoteRepository = obsidianNoteRepository;
        this.runtimeProperties = runtimeProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public WikiIngestWriteResult writeWikiIngestSourcePage(String runUid, SourceFileRecord sourceFile) {
        SourceContent sourceContent = sourceContentRepository.findBySourceFileUid(sourceFile.fileUid()).orElse(null);
        Path vaultRoot = vaultRoot();
        String sourcePagePath = wikiSourcePagePath(sourceFile);
        Path sourcePage = resolveVaultPath(vaultRoot, sourcePagePath);
        String managedBlock = wikiManagedBlock(runUid, sourceFile, sourceContent);
        String mergedMarkdown = mergeManagedBlock(readIfExists(sourcePage), managedBlock, managedBlockStart(sourceFile));
        writeAtomically(sourcePage, mergedMarkdown);

        String indexEntry = "- [[" + wikiLink(sourcePagePath) + "|" + title(sourceFile) + "]] - `"
                + sourceFile.fileUid()
                + "`";
        boolean indexUpdated = ensureLine(WIKI_INDEX_PATH, "# WikiForge Index\n\n## 来源\n", indexEntry);

        String logEntry = "- "
                + OffsetDateTime.now()
                + " 写入来源页 `"
                + sourceFile.fileUid()
                + "` -> [["
                + wikiLink(sourcePagePath)
                + "|"
                + title(sourceFile)
                + "]] (`"
                + runUid
                + "`)";
        appendLogLine(WIKI_LOG_PATH, "# WikiForge Log\n\n", logEntry);

        LocalDateTime now = LocalDateTime.now();
        String contentHash = sha256(mergedMarkdown);
        ObsidianNote note = obsidianNoteRepository.save(new ObsidianNote(
                null,
                nextNoteUid(),
                sourceFile.sourceId(),
                sourceFile.sourceFileId(),
                WIKI_SOURCE_PAGE_TYPE,
                runtimeProperties.obsidianVaultName(),
                sourcePagePath,
                sourcePage.toString(),
                obsidianUri(runtimeProperties.obsidianVaultName(), sourcePagePath),
                title(sourceFile),
                frontmatterJson(sourceFile, contentHash),
                contentHash,
                WRITTEN_STATUS,
                now,
                now
        ));

        return new WikiIngestWriteResult(
                sourcePagePath,
                List.of(sourcePagePath),
                indexUpdated,
                true,
                managedBlock,
                logEntry,
                note.obsidianUri()
        );
    }

    public ObsidianInitResponse initializeVault() {
        Path vaultRoot = vaultRoot();
        List<String> createdDirectories = VAULT_DIRECTORIES.stream()
                .peek(directory -> createDirectory(resolveVaultPath(vaultRoot, directory)))
                .toList();
        return new ObsidianInitResponse(runtimeProperties.obsidianVaultName(), vaultRoot.toString(), createdDirectories);
    }

    @Transactional(readOnly = true)
    public ObsidianVaultStatusResponse status() {
        String configuredPath = runtimeProperties.obsidianVaultPath();
        ObsidianNote latestNote = obsidianNoteRepository.findLatest().orElse(null);
        OffsetDateTime lastWrittenAt = latestNote == null ? null : toOffset(latestNote.createdAt());
        String lastNoteUid = latestNote == null ? null : latestNote.noteUid();
        if (configuredPath == null || configuredPath.isBlank()) {
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    configuredPath,
                    false,
                    false,
                    false,
                    lastNoteUid,
                    lastWrittenAt,
                    "obsidian vault path is not configured"
            );
        }
        try {
            Path root = PathSafety.normalizeAbsolute(Path.of(configuredPath));
            boolean exists = Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS);
            Path sourceNoteDirectory = root.resolve(SOURCE_NOTE_DIRECTORY).normalize();
            boolean sourceNoteDirectoryExists = sourceNoteDirectory.startsWith(root)
                    && Files.isDirectory(sourceNoteDirectory, LinkOption.NOFOLLOW_LINKS);
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    root.toString(),
                    exists,
                    exists && Files.isWritable(root),
                    sourceNoteDirectoryExists,
                    lastNoteUid,
                    lastWrittenAt,
                    null
            );
        } catch (RuntimeException exception) {
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    configuredPath,
                    false,
                    false,
                    false,
                    lastNoteUid,
                    lastWrittenAt,
                    exception.getMessage()
            );
        }
    }

    @Transactional(readOnly = true)
    public SourceNoteDraftResponse generateDraft(String fileUid) {
        SourceFileRecord sourceFile = findSourceFile(fileUid);
        return buildDraft(sourceFile);
    }

    @Transactional(readOnly = true)
    public ObsidianNoteResponse findSourceFileNote(String fileUid) {
        SourceFileRecord sourceFile = findSourceFile(fileUid);
        return obsidianNoteRepository.findBySourceFileUid(fileUid)
                .map(note -> toResponse(note, sourceFile))
                .orElse(null);
    }

    @Transactional
    public ObsidianNoteResponse writeSourceNote(String fileUid, String markdown) {
        SourceFileRecord sourceFile = findSourceFile(fileUid);
        SourceNoteDraftResponse draft = buildDraft(sourceFile);
        String noteMarkdown = markdown == null || markdown.isBlank() ? draft.markdown() : markdown;
        Path vaultRoot = vaultRoot();
        Path target = resolveVaultPath(vaultRoot, draft.vaultPath());

        writeAtomically(target, noteMarkdown);

        LocalDateTime now = LocalDateTime.now();
        String contentHash = sha256(noteMarkdown);
        ObsidianNote note = obsidianNoteRepository.save(new ObsidianNote(
                null,
                nextNoteUid(),
                sourceFile.sourceId(),
                sourceFile.sourceFileId(),
                SOURCE_NOTE_TYPE,
                draft.vaultName(),
                draft.vaultPath(),
                target.toString(),
                obsidianUri(draft.vaultName(), draft.vaultPath()),
                draft.title(),
                frontmatterJson(sourceFile, contentHash),
                contentHash,
                WRITTEN_STATUS,
                now,
                now
        ));
        return toResponse(note, sourceFile);
    }

    @Transactional(readOnly = true)
    public ObsidianNotePreviewResponse preview(String noteUid) {
        ObsidianNote note = obsidianNoteRepository.findByNoteUid(noteUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.OBSIDIAN_NOTE_NOT_FOUND));
        Path notePath = resolveVaultPath(vaultRoot(), note.vaultPath());
        if (!Files.isRegularFile(notePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new BusinessException(ErrorCode.OBSIDIAN_NOTE_NOT_FOUND, "obsidian note file not found");
        }
        try {
            return new ObsidianNotePreviewResponse(
                    note.noteUid(),
                    note.title(),
                    note.vaultName(),
                    note.vaultPath(),
                    note.obsidianUri(),
                    Files.readString(notePath, StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note cannot be read");
        }
    }

    private SourceFileRecord findSourceFile(String fileUid) {
        return sourceFileRepository.findByFileUid(fileUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND));
    }

    private SourceNoteDraftResponse buildDraft(SourceFileRecord sourceFile) {
        String title = title(sourceFile);
        String vaultName = runtimeProperties.obsidianVaultName();
        String vaultPath = SOURCE_NOTE_DIRECTORY + "/" + safeFileName(title) + "-" + sourceFile.sourceUid() + ".md";
        return new SourceNoteDraftResponse(
                sourceFile.fileUid(),
                sourceFile.sourceUid(),
                title,
                vaultName,
                vaultPath,
                markdown(sourceFile, title, sourceContentRepository.findBySourceFileUid(sourceFile.fileUid()).orElse(null))
        );
    }

    private String markdown(SourceFileRecord sourceFile, String title, SourceContent sourceContent) {
        String today = LocalDate.now().toString();
        return """
                ---
                id: %s
                source_file_uid: %s
                title: %s
                source_type: %s
                source_platform: local
                original_path: %s
                managed_path: %s
                content_hash: %s
                collected_at: %s
                status: draft
                tags:
                  - wikiforge/source-note
                  - source/%s
                ---
                # %s

                ## 原始资料 Source

                - Source UID: `%s`
                - Source File UID: `%s`
                - 文件名: `%s`
                - 文件类型: `%s`
                - 原始路径: `%s`
                - 归档路径: `%s`
                - 内容哈希: `%s`

                ## 摘要 Summary

                待补充：MVP2 先完成 Source Note 归档闭环，AI 摘要将在后续版本生成。

                %s

                ## 关键内容 Key Points

                - 待补充

                ## 后续处理 Next Actions

                - [ ] 人工检查资料归档是否正确
                - [ ] 后续进入知识提炼或 Wiki 编译流程
                """.formatted(
                sourceFile.sourceUid(),
                sourceFile.fileUid(),
                yamlValue(title),
                yamlScalar(sourceFile.fileExt()),
                yamlValue(sourceFile.originalPath()),
                yamlValue(sourceFile.managedPath()),
                yamlValue(sourceFile.contentHash()),
                today,
                tagValue(sourceFile.fileExt()),
                title,
                sourceFile.sourceUid(),
                sourceFile.fileUid(),
                value(sourceFile.fileName()),
                value(sourceFile.fileExt()),
                value(sourceFile.originalPath()),
                value(sourceFile.managedPath()),
                value(sourceFile.contentHash()),
                excerptSection(sourceContent)
        );
    }

    private String excerptSection(SourceContent sourceContent) {
        if (sourceContent == null || sourceContent.rawText() == null || sourceContent.rawText().isBlank()) {
            return "";
        }
        return "## 正文摘录 Content Excerpt\n\n" + truncate(sourceContent.rawText().trim(), 2000) + "\n";
    }

    private String wikiSourcePagePath(SourceFileRecord sourceFile) {
        LocalDate today = LocalDate.now();
        String year = today.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = today.format(DateTimeFormatter.ofPattern("MM"));
        return WIKI_SOURCE_DIRECTORY
                + "/"
                + year
                + "/"
                + month
                + "/"
                + sourceFile.fileUid()
                + "-"
                + safeFileName(title(sourceFile))
                + ".md";
    }

    private String wikiManagedBlock(String runUid, SourceFileRecord sourceFile, SourceContent sourceContent) {
        String excerpt = sourceContent == null || sourceContent.rawText() == null || sourceContent.rawText().isBlank()
                ? "暂无正文抽取结果。"
                : truncate(sourceContent.rawText().trim(), 2000);
        return """
                %s
                # %s

                ## 来源

                - Source UID: `%s`
                - Source File UID: `%s`
                - 文件名: `%s`
                - 文件类型: `%s`
                - Raw Sources: `%s`
                - 内容哈希: `%s`
                - 写入运行: `%s`

                ## 摘要

                MVP0 先按规则写入来源页托管区块，后续再接入 LLM 摘要和主题归档。

                ## 正文摘录

                %s
                %s
                """.formatted(
                managedBlockStart(sourceFile),
                title(sourceFile),
                sourceFile.sourceUid(),
                sourceFile.fileUid(),
                value(sourceFile.fileName()),
                value(sourceFile.fileExt()),
                value(sourceFile.managedPath()),
                value(sourceFile.contentHash()),
                runUid,
                excerpt,
                managedBlockEnd()
        );
    }

    private String managedBlockStart(SourceFileRecord sourceFile) {
        return "<!-- wikiforge:managed:start source_file_uid=" + sourceFile.fileUid() + " -->";
    }

    private String managedBlockEnd() {
        return "<!-- wikiforge:managed:end -->";
    }

    private String mergeManagedBlock(String existing, String managedBlock, String startMarker) {
        if (existing == null || existing.isBlank()) {
            return managedBlock;
        }
        int startIndex = existing.indexOf(startMarker);
        if (startIndex < 0) {
            return existing.stripTrailing() + "\n\n" + managedBlock;
        }
        int endIndex = existing.indexOf(managedBlockEnd(), startIndex);
        if (endIndex < 0) {
            return existing.stripTrailing() + "\n\n" + managedBlock;
        }
        int afterEnd = endIndex + managedBlockEnd().length();
        return existing.substring(0, startIndex) + managedBlock + existing.substring(afterEnd);
    }

    private String readIfExists(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note cannot be read");
        }
    }

    private boolean ensureLine(String vaultPath, String header, String line) {
        Path target = resolveVaultPath(vaultRoot(), vaultPath);
        String existing = readIfExists(target);
        if (existing.contains(line)) {
            return false;
        }
        String next = existing.isBlank()
                ? header + line + "\n"
                : existing.stripTrailing() + "\n" + line + "\n";
        writeAtomically(target, next);
        return true;
    }

    private void appendLogLine(String vaultPath, String header, String line) {
        Path target = resolveVaultPath(vaultRoot(), vaultPath);
        String existing = readIfExists(target);
        String next = existing.isBlank()
                ? header + line + "\n"
                : existing.stripTrailing() + "\n" + line + "\n";
        writeAtomically(target, next);
    }

    private String wikiLink(String vaultPath) {
        String prefix = MANAGED_ROOT + "/";
        return vaultPath.startsWith(prefix) ? vaultPath.substring(prefix.length()) : vaultPath;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim() + "\n\n...";
    }

    private Path vaultRoot() {
        String configuredPath = runtimeProperties.obsidianVaultPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path is not configured");
        }
        Path root = PathSafety.normalizeAbsolute(Path.of(configuredPath));
        try {
            Files.createDirectories(root);
            return root.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path cannot be created");
        }
    }

    private Path resolveVaultPath(Path vaultRoot, String vaultPath) {
        if (vaultPath == null || vaultPath.isBlank()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path is blank");
        }
        Path relativePath = Path.of(vaultPath);
        if (relativePath.isAbsolute()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note path must be relative");
        }
        Path resolved = vaultRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(vaultRoot)) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note path escapes vault");
        }
        return resolved;
    }

    private void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian directory cannot be created");
        }
    }

    private void writeAtomically(Path target, String markdown) {
        createDirectory(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".wf.tmp");
        try {
            Files.writeString(temp, markdown, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note cannot be written");
        }
    }

    private ObsidianNoteResponse toResponse(ObsidianNote note, SourceFileRecord sourceFile) {
        return new ObsidianNoteResponse(
                note.noteUid(),
                sourceFile.fileUid(),
                sourceFile.sourceUid(),
                note.title(),
                note.vaultName(),
                note.vaultPath(),
                note.absolutePath(),
                note.obsidianUri(),
                note.contentHash(),
                note.status(),
                toOffset(note.createdAt())
        );
    }

    private String frontmatterJson(SourceFileRecord sourceFile, String contentHash) {
        try {
            Map<String, Object> frontmatter = new LinkedHashMap<>();
            frontmatter.put("sourceUid", sourceFile.sourceUid());
            frontmatter.put("fileUid", sourceFile.fileUid());
            frontmatter.put("title", title(sourceFile));
            frontmatter.put("contentHash", contentHash);
            return objectMapper.writeValueAsString(frontmatter);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "frontmatter json cannot be generated");
        }
    }

    private String obsidianUri(String vaultName, String vaultPath) {
        return "obsidian://open?vault=" + encode(vaultName) + "&file=" + encode(vaultPath);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String nextNoteUid() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "note_" + date + "_" + suffix;
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String title(SourceFileRecord sourceFile) {
        if (sourceFile.fileName() == null || sourceFile.fileName().isBlank()) {
            return sourceFile.sourceUid();
        }
        return sourceFile.fileName();
    }

    private String safeFileName(String value) {
        String safe = value == null ? "untitled" : value;
        safe = safe.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "-")
                .replaceAll("\\s+", " ")
                .trim();
        if (safe.isBlank()) {
            safe = "untitled";
        }
        return safe.length() > 80 ? safe.substring(0, 80).trim() : safe;
    }

    private String tagValue(String value) {
        if (value == null || value.isBlank()) {
            return "file";
        }
        String tag = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        return tag.isBlank() ? "file" : tag;
    }

    private String yamlScalar(String value) {
        return value == null || value.isBlank() ? "file" : tagValue(value);
    }

    private String yamlValue(String value) {
        return "\"" + value(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record WikiIngestWriteResult(
            String sourcePagePath,
            List<String> wikiPagePaths,
            boolean indexUpdated,
            boolean logEntryAppended,
            String managedBlockPreview,
            String logEntryPreview,
            String obsidianUri
    ) {
    }
}
