package com.wikiforge.worker.infrastructure.filesystem;

import com.wikiforge.worker.domain.model.LocalScanFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RawSourceFileCollector {

    static final String PARSE_STATUS_PENDING = "pending";
    static final String ORGANIZE_MODE_COPY = "copy";
    static final String ORGANIZE_MODE_MOVE = "move";
    static final String ORGANIZE_MODE_REFERENCE = "reference";
    static final String ORGANIZE_STATUS_COPIED = "copied";
    static final String ORGANIZE_STATUS_MOVED = "moved";
    static final String ORGANIZE_STATUS_REFERENCED = "referenced";
    static final String ORGANIZE_STATUS_DUPLICATE = "duplicate";

    private final FileContentHasher fileContentHasher;
    private final FileTypeDetector fileTypeDetector;

    public RawSourceFileCollector() {
        this(new FileContentHasher(), new FileTypeDetector());
    }

    public RawSourceFileCollector(FileContentHasher fileContentHasher, FileTypeDetector fileTypeDetector) {
        this.fileContentHasher = fileContentHasher;
        this.fileTypeDetector = fileTypeDetector;
    }

    LocalScanFile collect(
            Path file,
            Path rawSourcesRoot,
            Map<String, LocalScanFile> collectedByHash,
            String organizeMode
    ) throws IOException {
        String contentHash = fileContentHasher.sha256(file);
        LocalScanFile duplicateOf = collectedByHash.get(contentHash);
        if (duplicateOf != null) {
            return toDuplicate(file, contentHash, duplicateOf);
        }

        String normalizedMode = normalizeOrganizeMode(organizeMode);
        if (ORGANIZE_MODE_REFERENCE.equals(normalizedMode)) {
            LocalScanFile scannedFile = toReferencedFile(file, contentHash);
            collectedByHash.put(contentHash, scannedFile);
            return scannedFile;
        }

        Path destination = nextAvailableDestination(rawSourcesRoot.resolve(fileTypeDetector.categoryFolder(file)), file);
        Files.createDirectories(destination.getParent());
        LocalScanFile scannedFile;
        if (ORGANIZE_MODE_MOVE.equals(normalizedMode)) {
            Files.move(file, destination);
            scannedFile = toCollectedFile(file, destination, contentHash, ORGANIZE_STATUS_MOVED);
        } else {
            Files.copy(file, destination, StandardCopyOption.COPY_ATTRIBUTES);
            scannedFile = toCollectedFile(file, destination, contentHash, ORGANIZE_STATUS_COPIED);
        }
        collectedByHash.put(contentHash, scannedFile);
        return scannedFile;
    }

    private LocalScanFile toCollectedFile(
            Path file,
            Path destination,
            String contentHash,
            String organizeStatus
    ) throws IOException {
        return new LocalScanFile(
                newFileUid(),
                file.getFileName().toString(),
                fileTypeDetector.fileExt(file),
                file.toAbsolutePath().normalize().toString(),
                destination.toAbsolutePath().normalize().toString(),
                Files.size(destination),
                fileTypeDetector.mimeType(destination),
                contentHash,
                PARSE_STATUS_PENDING,
                organizeStatus,
                null
        );
    }

    private LocalScanFile toReferencedFile(Path file, String contentHash) throws IOException {
        Path normalizedFile = file.toAbsolutePath().normalize();
        return new LocalScanFile(
                newFileUid(),
                file.getFileName().toString(),
                fileTypeDetector.fileExt(file),
                normalizedFile.toString(),
                normalizedFile.toString(),
                Files.size(file),
                fileTypeDetector.mimeType(file),
                contentHash,
                PARSE_STATUS_PENDING,
                ORGANIZE_STATUS_REFERENCED,
                null
        );
    }

    private LocalScanFile toDuplicate(Path file, String contentHash, LocalScanFile duplicateOf) throws IOException {
        return new LocalScanFile(
                newFileUid(),
                file.getFileName().toString(),
                fileTypeDetector.fileExt(file),
                file.toAbsolutePath().normalize().toString(),
                duplicateOf.managedPath(),
                Files.size(file),
                fileTypeDetector.mimeType(file),
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

    private String newFileUid() {
        return "file_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeOrganizeMode(String organizeMode) {
        if (organizeMode == null || organizeMode.isBlank()) {
            return ORGANIZE_MODE_COPY;
        }
        return switch (organizeMode) {
            case ORGANIZE_MODE_COPY, ORGANIZE_MODE_MOVE, ORGANIZE_MODE_REFERENCE -> organizeMode;
            default -> throw new IllegalArgumentException("unsupported organizeMode: " + organizeMode);
        };
    }
}
