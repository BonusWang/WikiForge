package com.wikiforge.core.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.ObsidianInitResponse;
import com.wikiforge.core.application.dto.ObsidianVaultStatusResponse;
import com.wikiforge.core.domain.model.SourceContent;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.repository.SourceContentRepository;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunEntity;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObsidianVaultService {

    private static final String MANAGED_ROOT = "WikiForge";
    private static final String MANAGED_ROOT_DISPLAY = MANAGED_ROOT + "/";
    private static final String WIKI_SOURCE_DIRECTORY = MANAGED_ROOT + "/10_来源";
    private static final String WIKI_INDEX_PATH = MANAGED_ROOT + "/index.md";
    private static final String WIKI_LOG_PATH = MANAGED_ROOT + "/log.md";
    private static final List<String> VAULT_DIRECTORIES = List.of(
            MANAGED_ROOT,
            MANAGED_ROOT + "/00_规则",
            WIKI_SOURCE_DIRECTORY
    );
    private static final Map<String, String> VAULT_FILES = vaultFiles();

    private final SourceContentRepository sourceContentRepository;
    private final WikiIngestRunMapper wikiIngestRunMapper;
    private final CoreRuntimeProperties runtimeProperties;

    public ObsidianVaultService(
            SourceContentRepository sourceContentRepository,
            WikiIngestRunMapper wikiIngestRunMapper,
            CoreRuntimeProperties runtimeProperties
    ) {
        this.sourceContentRepository = sourceContentRepository;
        this.wikiIngestRunMapper = wikiIngestRunMapper;
        this.runtimeProperties = runtimeProperties;
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

        return new WikiIngestWriteResult(
                sourcePagePath,
                indexUpdated,
                true,
                managedBlock,
                logEntry,
                obsidianUri(runtimeProperties.obsidianVaultName(), sourcePagePath)
        );
    }

    public ObsidianInitResponse initializeVault() {
        Path vaultRoot = vaultRoot();
        List<String> createdPaths = new ArrayList<>();
        for (String directory : VAULT_DIRECTORIES) {
            createDirectory(resolveVaultPath(vaultRoot, directory));
            createdPaths.add(directory + "/");
        }
        for (Map.Entry<String, String> file : VAULT_FILES.entrySet()) {
            ensureFile(resolveVaultPath(vaultRoot, file.getKey()), file.getValue());
            createdPaths.add(file.getKey());
        }
        return new ObsidianInitResponse(runtimeProperties.obsidianVaultName(), MANAGED_ROOT_DISPLAY, createdPaths);
    }

    @Transactional(readOnly = true)
    public ObsidianVaultStatusResponse status() {
        String configuredPath = runtimeProperties.obsidianVaultPath();
        OffsetDateTime lastWriteAt = latestWikiWriteAt();
        if (configuredPath == null || configuredPath.isBlank()) {
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    null,
                    MANAGED_ROOT_DISPLAY,
                    false,
                    false,
                    false,
                    lastWriteAt,
                    "obsidian vault path is not configured"
            );
        }
        try {
            Path root = PathSafety.normalizeAbsolute(Path.of(configuredPath));
            boolean exists = Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS);
            Path managedRoot = root.resolve(MANAGED_ROOT).normalize();
            boolean managedRootExists = managedRoot.startsWith(root)
                    && Files.isDirectory(managedRoot, LinkOption.NOFOLLOW_LINKS);
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    maskPath(root),
                    MANAGED_ROOT_DISPLAY,
                    exists,
                    exists && Files.isWritable(root),
                    managedRootExists,
                    lastWriteAt,
                    null
            );
        } catch (RuntimeException exception) {
            return new ObsidianVaultStatusResponse(
                    runtimeProperties.obsidianVaultName(),
                    maskConfiguredPath(configuredPath),
                    MANAGED_ROOT_DISPLAY,
                    false,
                    false,
                    false,
                    lastWriteAt,
                    exception.getMessage()
            );
        }
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

                - Source File UID: `%s`
                - 文件名: `%s`
                - 文件类型: `%s`
                - Raw Sources: `%s`
                - 内容哈希: `%s`
                - 写入运行: `%s`

                ## 摘要

                MVP0 当前按规则写入来源页托管区块。

                ## 正文摘录

                %s
                %s
                """.formatted(
                managedBlockStart(sourceFile),
                title(sourceFile),
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
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note path is blank");
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

    private void ensureFile(Path target, String content) {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        writeAtomically(target, content);
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

    private OffsetDateTime latestWikiWriteAt() {
        WikiIngestRunEntity latest = wikiIngestRunMapper.selectOne(
                new LambdaQueryWrapper<WikiIngestRunEntity>()
                        .eq(WikiIngestRunEntity::getStatusCode, "已写入")
                        .orderByDesc(WikiIngestRunEntity::getCompletedAt)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            return null;
        }
        LocalDateTime writtenAt = latest.getCompletedAt() == null ? latest.getUpdatedAt() : latest.getCompletedAt();
        return toOffset(writtenAt);
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String obsidianUri(String vaultName, String vaultPath) {
        return "obsidian://open?vault=" + encode(vaultName) + "&file=" + encode(vaultPath);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String title(SourceFileRecord sourceFile) {
        if (sourceFile.fileName() == null || sourceFile.fileName().isBlank()) {
            return sourceFile.fileUid();
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

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String maskPath(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? "***" : ".../" + fileName;
    }

    private String maskConfiguredPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        return maskPath(Path.of(configuredPath).normalize());
    }

    private static Map<String, String> vaultFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(WIKI_INDEX_PATH, "# WikiForge Index\n\n## 来源\n");
        files.put(WIKI_LOG_PATH, "# WikiForge Log\n\n");
        files.put(MANAGED_ROOT + "/00_规则/LLM-Wiki写入规则.md",
                "# LLM Wiki 写入规则\n\nWikiForge 只维护带 `wikiforge:managed` 标记的托管区块。\n");
        files.put(MANAGED_ROOT + "/00_规则/来源页模板.md", "# 来源页模板\n\n用于 Raw Sources 对应来源页。\n");
        return files;
    }

    public record WikiIngestWriteResult(
            String sourcePagePath,
            boolean indexUpdated,
            boolean logEntryAppended,
            String managedBlockPreview,
            String logEntryPreview,
            String obsidianUri
    ) {
    }
}
