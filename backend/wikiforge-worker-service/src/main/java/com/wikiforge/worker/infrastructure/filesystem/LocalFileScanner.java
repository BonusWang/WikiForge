package com.wikiforge.worker.infrastructure.filesystem;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.worker.domain.model.LocalScanFile;
import com.wikiforge.worker.domain.model.LocalScanRequest;
import com.wikiforge.worker.domain.model.LocalScanResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocalFileScanner {

    private final RawSourceFileCollector rawSourceFileCollector;

    public LocalFileScanner() {
        this(new RawSourceFileCollector());
    }

    @Autowired
    public LocalFileScanner(RawSourceFileCollector rawSourceFileCollector) {
        this.rawSourceFileCollector = rawSourceFileCollector;
    }

    public LocalScanResult scan(LocalScanRequest request) {
        Path inputPath = PathSafety.normalizeAbsolute(request.inputPath());
        Path rawSourcesRoot = PathSafety.normalizeAbsolute(request.rawSourcesRoot());
        PathSafety.ensureNoOverlap(inputPath, rawSourcesRoot);
        if (!Files.exists(inputPath)) {
            throw new BusinessException(ErrorCode.SOURCE_PATH_NOT_FOUND);
        }
        if (!Files.isDirectory(inputPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new BusinessException(ErrorCode.SOURCE_UNSUPPORTED_INPUT_TYPE);
        }
        if (request.followSymlinks()) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "followSymlinks must be false in MVP1");
        }

        try {
            Files.createDirectories(rawSourcesRoot);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, exception.getMessage());
        }

        ScanAccumulator accumulator = new ScanAccumulator(request);
        scanDirectory(inputPath, rawSourcesRoot, request, accumulator, true);
        return new LocalScanResult(
                accumulator.totalCount,
                accumulator.successCount,
                accumulator.skippedCount,
                accumulator.failedCount,
                List.copyOf(accumulator.files)
        );
    }

    private void scanDirectory(
            Path directory,
            Path rawSourcesRoot,
            LocalScanRequest request,
            ScanAccumulator accumulator,
            boolean root
    ) {
        if (!root && shouldSkip(directory, request)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            List<Path> children = stream.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            for (Path child : children) {
                if (isLinkOrOther(child)) {
                    accumulator.skippedCount++;
                    continue;
                }
                if (!Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    scanChildFile(child, rawSourcesRoot, request, accumulator);
                }
            }
            for (Path child : children) {
                if (!isLinkOrOther(child) && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) && request.recursive()) {
                    scanDirectory(child, rawSourcesRoot, request, accumulator, false);
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, exception.getMessage());
        }
    }

    private void scanChildFile(Path child, Path rawSourcesRoot, LocalScanRequest request, ScanAccumulator accumulator) {
        if (isLinkOrOther(child)) {
            accumulator.skippedCount++;
            return;
        }
        if (!Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)) {
            accumulator.skippedCount++;
            return;
        }
        if (shouldSkip(child, request)) {
            accumulator.skippedCount++;
            return;
        }
        scanFile(child, rawSourcesRoot, accumulator);
    }

    private void scanFile(Path file, Path rawSourcesRoot, ScanAccumulator accumulator) {
        accumulator.totalCount++;
        try {
            long fileSize = Files.size(file);
            if (fileSize > maxCopyBytes(accumulator.request)) {
                accumulator.skippedCount++;
                return;
            }
            LocalScanFile scannedFile = rawSourceFileCollector.collect(file, rawSourcesRoot, accumulator.copiedByHash);
            accumulator.files.add(scannedFile);
            if (RawSourceFileCollector.ORGANIZE_STATUS_COPIED.equals(scannedFile.organizeStatus())) {
                accumulator.successCount++;
            }
        } catch (IOException exception) {
            accumulator.failedCount++;
        }
    }

    private boolean shouldSkip(Path path, LocalScanRequest request) {
        return (request.skipHidden() && isHidden(path))
                || (request.skipTemporary() && isTemporary(path.getFileName().toString()));
    }

    private boolean isLinkOrOther(Path path) {
        if (Files.isSymbolicLink(path)) {
            return true;
        }
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther();
        } catch (IOException exception) {
            return true;
        }
    }

    private boolean isHidden(Path path) {
        if (path.getFileName().toString().startsWith(".")) {
            return true;
        }
        try {
            return Files.isHidden(path);
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean isTemporary(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return fileName.startsWith("~$")
                || lowerName.endsWith(".tmp")
                || lowerName.endsWith(".temp")
                || lowerName.endsWith(".swp")
                || ".ds_store".equals(lowerName)
                || "thumbs.db".equals(lowerName);
    }

    private long maxCopyBytes(LocalScanRequest request) {
        return request.maxCopyFileSizeMb() * 1024L * 1024L;
    }

    private static final class ScanAccumulator {
        private final List<LocalScanFile> files = new ArrayList<>();
        private final Map<String, LocalScanFile> copiedByHash = new HashMap<>();
        private final LocalScanRequest request;
        private int totalCount;
        private int successCount;
        private int skippedCount;
        private int failedCount;

        private ScanAccumulator(LocalScanRequest request) {
            this.request = request;
        }
    }
}
