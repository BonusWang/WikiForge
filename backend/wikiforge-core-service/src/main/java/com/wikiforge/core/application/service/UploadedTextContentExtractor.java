package com.wikiforge.core.application.service;

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
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class UploadedTextContentExtractor {

    private static final int MAX_TEXT_CHARS = 1_000_000;

    public Optional<ParsedUploadTextContent> extract(Path managedPath, String fileExt) {
        String extension = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        if (!List.of("md", "txt", "pdf", "docx").contains(extension)) {
            return Optional.empty();
        }
        try {
            String parsedText = switch (extension) {
                case "md" -> stripFrontmatter(Files.readString(managedPath, StandardCharsets.UTF_8));
                case "txt" -> Files.readString(managedPath, StandardCharsets.UTF_8);
                case "pdf" -> extractPdfText(managedPath);
                case "docx" -> extractDocxText(managedPath);
                default -> throw new IllegalArgumentException("unsupported extension");
            };
            return Optional.of(success(parserName(extension), parsedText));
        } catch (IOException | RuntimeException exception) {
            return Optional.of(failure(parserName(extension), exception));
        }
    }

    private String extractPdfText(Path managedPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(managedPath.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocxText(Path managedPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(managedPath))) {
            return document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        }
    }

    private ParsedUploadTextContent success(String parserName, String rawText) {
        String parsedText = rawText == null ? "" : rawText;
        boolean partial = parsedText.length() > MAX_TEXT_CHARS;
        parsedText = truncate(parsedText);
        return new ParsedUploadTextContent(
                parserName,
                "plain_text",
                parsedText,
                sha256(parsedText),
                parsedText.length(),
                true,
                partial ? "partial" : "success",
                null
        );
    }

    private ParsedUploadTextContent failure(String parserName, Exception exception) {
        return new ParsedUploadTextContent(
                parserName,
                "plain_text",
                null,
                null,
                0,
                false,
                "failed",
                exception.getMessage()
        );
    }

    private String parserName(String extension) {
        return switch (extension) {
            case "md" -> "markdown-text";
            case "txt" -> "plain-text";
            case "pdf" -> "pdfbox-text";
            case "docx" -> "poi-docx-text";
            default -> "unsupported-text";
        };
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

    public record ParsedUploadTextContent(
            String parserName,
            String contentType,
            String parsedText,
            String textHash,
            Integer charCount,
            Boolean rawTextSaved,
            String parseStatus,
            String parseError
    ) {
    }
}
