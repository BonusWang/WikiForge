package com.wikiforge.worker.infrastructure.filesystem;

import com.wikiforge.worker.domain.model.LocalScanFile;
import com.wikiforge.worker.domain.model.LocalScanRequest;
import com.wikiforge.worker.domain.model.LocalScanResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileScannerTests {

    @TempDir
    Path tempDir;

    @Test
    void scanCopiesSupportedFilesByTypeAndMarksHashDuplicates() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path nestedPath = inputPath.resolve("nested");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(nestedPath);
        Files.createDirectories(rawSourcesRoot);

        Path document = inputPath.resolve("note.md");
        Path image = nestedPath.resolve("photo.jpg");
        Path pdf = inputPath.resolve("paper.pdf");
        Path unknown = inputPath.resolve("archive.bin");
        Path duplicate = nestedPath.resolve("note-copy.md");
        Files.writeString(document, "same content", StandardCharsets.UTF_8);
        Files.writeString(duplicate, "same content", StandardCharsets.UTF_8);
        Files.writeString(image, "image-bytes", StandardCharsets.UTF_8);
        Files.writeString(pdf, "pdf-bytes", StandardCharsets.UTF_8);
        Files.writeString(unknown, "unknown-bytes", StandardCharsets.UTF_8);

        LocalFileScanner scanner = new LocalFileScanner();

        LocalScanResult result = scanner.scan(new LocalScanRequest(
                inputPath,
                rawSourcesRoot,
                true,
                true,
                true,
                false,
                100
        ));

        assertThat(result.totalCount()).isEqualTo(5);
        assertThat(result.successCount()).isEqualTo(4);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.files()).hasSize(5);

        LocalScanFile copiedDocument = findByFileName(result, "note.md");
        LocalScanFile copiedImage = findByFileName(result, "photo.jpg");
        LocalScanFile copiedPdf = findByFileName(result, "paper.pdf");
        LocalScanFile copiedUnknown = findByFileName(result, "archive.bin");
        LocalScanFile duplicateDocument = findByFileName(result, "note-copy.md");

        assertThat(copiedDocument.contentHash()).isEqualTo(sha256("same content"));
        assertThat(copiedDocument.organizeStatus()).isEqualTo("copied");
        assertThat(duplicateDocument.contentHash()).isEqualTo(copiedDocument.contentHash());
        assertThat(duplicateDocument.organizeStatus()).isEqualTo("duplicate");
        assertThat(duplicateDocument.duplicateOfFileUid()).isNull();
        assertThat(duplicateDocument.managedPath()).isEqualTo(copiedDocument.managedPath());

        assertThat(Path.of(copiedDocument.managedPath())).isRegularFile();
        assertThat(Path.of(copiedImage.managedPath())).isRegularFile();
        assertThat(Path.of(copiedPdf.managedPath())).isRegularFile();
        assertThat(Path.of(copiedUnknown.managedPath())).isRegularFile();
        assertThat(Path.of(duplicateDocument.originalPath())).isRegularFile();

        assertThat(Path.of(copiedDocument.managedPath()).getParent().getFileName().toString())
                .isEqualTo("01_Documents_文档");
        assertThat(Path.of(copiedImage.managedPath()).getParent().getFileName().toString())
                .isEqualTo("02_Images_图片");
        assertThat(Path.of(copiedPdf.managedPath()).getParent().getFileName().toString())
                .isEqualTo("03_PDFs_PDF");
        assertThat(Path.of(copiedUnknown.managedPath()).getParent().getFileName().toString())
                .isEqualTo("90_Unknown_待确认");
    }

    @Test
    void scanSkipsHiddenTemporaryAndNestedFilesWhenConfigured() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path nestedPath = inputPath.resolve("nested");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(nestedPath);
        Files.createDirectories(rawSourcesRoot);

        Files.writeString(inputPath.resolve("visible.txt"), "visible", StandardCharsets.UTF_8);
        Files.writeString(inputPath.resolve(".hidden.txt"), "hidden", StandardCharsets.UTF_8);
        Files.writeString(inputPath.resolve("draft.tmp"), "temporary", StandardCharsets.UTF_8);
        Files.writeString(nestedPath.resolve("nested.txt"), "nested", StandardCharsets.UTF_8);

        LocalFileScanner scanner = new LocalFileScanner();

        LocalScanResult result = scanner.scan(new LocalScanRequest(
                inputPath,
                rawSourcesRoot,
                false,
                true,
                true,
                false,
                100
        ));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        assertThat(result.files())
                .extracting(LocalScanFile::fileName)
                .containsExactly("visible.txt");
    }

    @Test
    void scanSkipsFilesLargerThanMaxCopyFileSize() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(inputPath);
        Files.createDirectories(rawSourcesRoot);

        Files.write(inputPath.resolve("large.pdf"), new byte[1024 * 1024 + 1]);

        LocalFileScanner scanner = new LocalFileScanner();

        LocalScanResult result = scanner.scan(new LocalScanRequest(
                inputPath,
                rawSourcesRoot,
                true,
                true,
                true,
                false,
                1
        ));

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.files()).isEmpty();
        assertThat(rawSourcesRoot.resolve("03_PDFs_PDF").resolve("large.pdf")).doesNotExist();
    }

    private static LocalScanFile findByFileName(LocalScanResult result, String fileName) {
        return result.files().stream()
                .filter(file -> fileName.equals(file.fileName()))
                .findFirst()
                .orElseThrow();
    }

    private static String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
