package com.wikiforge.worker.application.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TextContentExtractor {

    private static final int MAX_TEXT_CHARS = 1_000_000;

    public Optional<ParsedTextContent> extract(Path managedPath, String fileExt) {
        String extension = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        if (!List.of("md", "txt").contains(extension)) {
            return Optional.empty();
        }
        try {
            String rawText = Files.readString(managedPath, StandardCharsets.UTF_8);
            String parsedText = "md".equals(extension) ? stripFrontmatter(rawText) : rawText;
            boolean partial = parsedText.length() > MAX_TEXT_CHARS;
            parsedText = truncate(parsedText);
            return Optional.of(new ParsedTextContent(
                    "md".equals(extension) ? "markdown-text" : "plain-text",
                    "plain_text",
                    parsedText,
                    sha256(parsedText),
                    parsedText.length(),
                    true,
                    partial ? "partial" : "success",
                    null
            ));
        } catch (IOException exception) {
            return Optional.of(new ParsedTextContent(
                    "md".equals(extension) ? "markdown-text" : "plain-text",
                    "plain_text",
                    null,
                    null,
                    0,
                    false,
                    "failed",
                    exception.getMessage()
            ));
        }
    }

    private String stripFrontmatter(String text) {
        String normalized = text.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return text;
        }
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            return text;
        }
        return normalized.substring(end + "\n---\n".length());
    }

    private String truncate(String text) {
        if (text.length() <= MAX_TEXT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_TEXT_CHARS);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
