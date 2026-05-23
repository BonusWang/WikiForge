package com.wikiforge.core.application.service;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.core.application.dto.CreateLinkSourceRequest;
import com.wikiforge.core.application.dto.LinkSourceResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkSourceService {

    private static final Set<String> SOURCE_TYPES = Set.of("link", "text", "note", "manual");
    private static final Set<String> PROCESSING_INTENTS = Set.of("organize_only", "extract_and_review");

    private final JdbcTemplate jdbcTemplate;

    public LinkSourceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public LinkSourceResponse createLinkSource(CreateLinkSourceRequest request) {
        String title = normalizeRequired(request.title(), "title");
        String sourceUrl = normalizeRequired(request.sourceUrl(), "sourceUrl");
        if (title.length() > 512 || sourceUrl.length() > 2048) {
            throw new BusinessException(ErrorCode.LINK_SOURCE_INVALID_INPUT);
        }
        String sourcePlatform = normalizeOptional(request.sourcePlatform());
        if (sourcePlatform == null) {
            sourcePlatform = inferPlatform(sourceUrl);
        }
        String sourceType = defaultText(request.sourceType(), "link");
        String processingIntent = defaultText(request.processingIntent(), "organize_only");
        requireOneOf(sourceType, SOURCE_TYPES, "sourceType");
        requireOneOf(processingIntent, PROCESSING_INTENTS, "processingIntent");
        if (sourcePlatform.length() > 128) {
            throw new BusinessException(ErrorCode.LINK_SOURCE_INVALID_INPUT);
        }
        String rawContent = normalizeOptional(request.rawContent());
        String content = rawContent == null ? linkPlaceholder(title, sourceUrl, sourcePlatform) : rawContent;
        if (content.length() > 100000) {
            throw new BusinessException(ErrorCode.LINK_SOURCE_INVALID_INPUT);
        }

        String sourceUid = nextUid("src");
        String fileUid = nextUid("file");
        String contentUid = nextUid("content");
        String jobUid = nextUid("job");
        LocalDateTime now = LocalDateTime.now();
        String contentHash = sha256(content);

        jdbcTemplate.update("""
                INSERT INTO import_jobs (
                    job_uid, import_type, input_url, status, total_count, success_count, skipped_count,
                    failed_count, created_at, updated_at
                ) VALUES (?, 'link_capture', ?, 'completed', 1, 1, 0, 0, ?, ?)
                """, jobUid, sourceUrl, now, now);
        Long importJobId = jdbcTemplate.queryForObject(
                "SELECT id FROM import_jobs WHERE job_uid = ?",
                Long.class,
                jobUid
        );

        jdbcTemplate.update("""
                INSERT INTO sources (
                    source_uid, title, source_type, source_platform, source_url, raw_organize_status,
                    processing_intent, content_hash, status, collected_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 'pending', ?, ?, 'pending', ?, ?, ?)
                """, sourceUid, title, sourceType, sourcePlatform, sourceUrl, processingIntent, contentHash, now, now, now);
        Long sourceId = jdbcTemplate.queryForObject(
                "SELECT id FROM sources WHERE source_uid = ?",
                Long.class,
                sourceUid
        );

        jdbcTemplate.update("""
                INSERT INTO source_files (
                    file_uid, source_id, import_job_id, file_name, file_ext, original_path, managed_path,
                    file_size, mime_type, content_hash, parser_name, parse_status, organize_status, created_at
                ) VALUES (?, ?, ?, ?, 'txt', ?, NULL, ?, 'text/plain', ?, 'link-capture', 'success', 'pending', ?)
                """,
                fileUid,
                sourceId,
                importJobId,
                safeFileName(title) + ".txt",
                sourceUrl,
                (long) content.length(),
                contentHash,
                now
        );
        Long sourceFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM source_files WHERE file_uid = ?",
                Long.class,
                fileUid
        );

        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    content_uid, source_id, source_file_id, parser_name, content_type, raw_text, text_hash,
                    char_count, raw_text_saved, parse_status, created_at, updated_at
                ) VALUES (?, ?, ?, 'link-capture', 'plain_text', ?, ?, ?, TRUE, 'success', ?, ?)
                """, contentUid, sourceId, sourceFileId, content, contentHash, content.length(), now, now);

        return new LinkSourceResponse(
                sourceUid,
                fileUid,
                jobUid,
                title,
                sourceUrl,
                sourcePlatform,
                "pending",
                toOffset(now)
        );
    }

    private String linkPlaceholder(String title, String sourceUrl, String sourcePlatform) {
        return """
                # %s

                - Source URL: %s
                - Source Platform: %s

                V1 link capture: content has not been fetched by a connector yet.
                """.formatted(title, sourceUrl, sourcePlatform);
    }

    private String inferPlatform(String sourceUrl) {
        String lower = sourceUrl.toLowerCase(Locale.ROOT);
        if (lower.contains("feishu") || lower.contains("larksuite")) {
            return "feishu";
        }
        if (lower.contains("docs.qq.com") || lower.contains("qq.com")) {
            return "tencent_doc";
        }
        if (lower.contains("bilibili.com")) {
            return "bilibili";
        }
        if (lower.contains("zhihu.com")) {
            return "zhihu";
        }
        if (lower.contains("weixin.qq.com") || lower.contains("mp.weixin.qq.com")) {
            return "wechat";
        }
        return "web";
    }

    private void requireOneOf(String value, Set<String> allowedValues, String fieldName) {
        if (!allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.LINK_SOURCE_INVALID_INPUT, fieldName + " is invalid");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.LINK_SOURCE_INVALID_INPUT, fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = normalizeOptional(value);
        return normalized == null ? defaultValue : normalized;
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

    private String nextUid(String prefix) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + date + "_" + suffix;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
