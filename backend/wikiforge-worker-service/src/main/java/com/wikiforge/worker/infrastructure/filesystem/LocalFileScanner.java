package com.wikiforge.worker.infrastructure.filesystem;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.worker.domain.model.LocalScanFile;
import com.wikiforge.worker.domain.model.LocalScanRequest;
import com.wikiforge.worker.domain.model.LocalScanResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalFileScanner {

    private static final String PARSE_STATUS_PENDING = "pending";
    private static final String ORGANIZE_STATUS_COPIED = "copied";
    private static final String ORGANIZE_STATUS_DUPLICATE = "duplicate";
    private static final String DOCUMENTS_FOLDER = "01_Documents_文档";
    private static final String IMAGES_FOLDER = "02_Images_图片";
    private static final String PDFS_FOLDER = "03_PDFs_PDF";
    private static final String UNKNOWN_FOLDER = "90_Unknown_待确认";

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
            String contentHash = sha256(file);
            LocalScanFile duplicateOf = accumulator.copiedByHash.get(contentHash);
            if (duplicateOf != null) {
                accumulator.files.add(toDuplicate(file, contentHash, duplicateOf));
                return;
            }

            Path destination = nextAvailableDestination(rawSourcesRoot.resolve(categoryFolder(file)), file);
            Files.createDirectories(destination.getParent());
            Files.copy(file, destination, StandardCopyOption.COPY_ATTRIBUTES);
            LocalScanFile scannedFile = toCopiedFile(file, destination, contentHash);
            accumulator.files.add(scannedFile);
            accumulator.copiedByHash.put(contentHash, scannedFile);
            accumulator.successCount++;
        } catch (IOException exception) {
            accumulator.failedCount++;
        }
    }

    private LocalScanFile toCopiedFile(Path file, Path destination, String contentHash) throws IOException {
        return new LocalScanFile(
                newFileUid(),
                file.getFileName().toString(),
                fileExt(file),
                file.toAbsolutePath().normalize().toString(),
                destination.toAbsolutePath().normalize().toString(),
                Files.size(file),
                mimeType(file),
                contentHash,
                PARSE_STATUS_PENDING,
                ORGANIZE_STATUS_COPIED,
                null
        );
    }

    private LocalScanFile toDuplicate(Path file, String contentHash, LocalScanFile duplicateOf) throws IOException {
        return new LocalScanFile(
                newFileUid(),
                file.getFileName().toString(),
                fileExt(file),
                file.toAbsolutePath().normalize().toString(),
                duplicateOf.managedPath(),
                Files.size(file),
                mimeType(file),
                contentHash,
                PARSE_STATUS_PENDING,
                ORGANIZE_STATUS_DUPLICATE,
                null
        );
    }

    private Path nextAvailableDestination(Path categoryRoot, Path file) {
        Path destination = categoryRoot.resolve(file.getFileName().toString());
        if (!Files.exists(destination)) {
            return destination;
        }

        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        int counter = 2;
        while (Files.exists(destination)) {
            destination = categoryRoot.resolve(baseName + "-" + counter + extension);
            counter++;
        }
        return destination;
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

    private String categoryFolder(Path file) {
        String extension = fileExt(file);
        if ("pdf".equals(extension)) {
            return PDFS_FOLDER;
        }
        if (List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff", "heic").contains(extension)) {
            return IMAGES_FOLDER;
        }
        if (List.of("md", "txt", "doc", "docx", "rtf", "csv", "xls", "xlsx", "ppt", "pptx", "odt").contains(extension)) {
            return DOCUMENTS_FOLDER;
        }
        return UNKNOWN_FOLDER;
    }

    private String mimeType(Path file) {
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                return probed;
            }
        } catch (IOException ignored) {
            // Fall through to extension-based MVP mapping.
        }

        return switch (fileExt(file)) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "md", "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String fileExt(Path file) {
        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(file);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // Read the stream so DigestInputStream can update the digest.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String newFileUid() {
        return "file_" + UUID.randomUUID().toString().replace("-", "");
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
