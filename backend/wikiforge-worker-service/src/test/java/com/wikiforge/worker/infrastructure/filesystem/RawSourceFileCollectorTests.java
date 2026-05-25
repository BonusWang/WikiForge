package com.wikiforge.worker.infrastructure.filesystem;

import com.wikiforge.worker.domain.model.LocalScanFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class RawSourceFileCollectorTests {

    @TempDir
    Path tempDir;

    @Test
    void collectCopiesFirstFileAndReusesManagedPathForHashDuplicate() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(inputPath);
        Files.createDirectories(rawSourcesRoot);
        Path first = inputPath.resolve("note.md");
        Path duplicate = inputPath.resolve("note-copy.md");
        Files.writeString(first, "same content", StandardCharsets.UTF_8);
        Files.writeString(duplicate, "same content", StandardCharsets.UTF_8);

        RawSourceFileCollector collector = new RawSourceFileCollector();
        Map<String, LocalScanFile> copiedByHash = new HashMap<>();

        LocalScanFile copied = collector.collect(first, rawSourcesRoot, copiedByHash);
        LocalScanFile duplicateResult = collector.collect(duplicate, rawSourcesRoot, copiedByHash);

        assertThat(copied.contentHash()).isEqualTo(sha256("same content"));
        assertThat(copied.fileExt()).isEqualTo("md");
        assertThat(copied.organizeStatus()).isEqualTo("copied");
        assertThat(Path.of(copied.managedPath())).isRegularFile();
        assertThat(Path.of(copied.managedPath()).getParent().getFileName().toString())
                .isEqualTo("01_Documents_文档");

        assertThat(duplicateResult.contentHash()).isEqualTo(copied.contentHash());
        assertThat(duplicateResult.organizeStatus()).isEqualTo("duplicate");
        assertThat(duplicateResult.managedPath()).isEqualTo(copied.managedPath());
        assertThat(rawSourcesRoot.resolve("01_Documents_文档").resolve("note-copy.md")).doesNotExist();
    }

    private static String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
