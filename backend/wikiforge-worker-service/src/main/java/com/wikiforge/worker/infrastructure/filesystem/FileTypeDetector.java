package com.wikiforge.worker.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FileTypeDetector {

    private static final String DOCUMENTS_FOLDER = "01_Documents_文档";
    private static final String IMAGES_FOLDER = "02_Images_图片";
    private static final String PDFS_FOLDER = "03_PDFs_PDF";
    private static final String UNKNOWN_FOLDER = "90_Unknown_待确认";

    String categoryFolder(Path file) {
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

    String mimeType(Path file) {
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

    String fileExt(Path file) {
        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
