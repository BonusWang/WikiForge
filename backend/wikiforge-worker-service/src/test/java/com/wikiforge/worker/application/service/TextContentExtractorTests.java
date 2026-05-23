package com.wikiforge.worker.application.service;

import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class TextContentExtractorTests {

    @TempDir
    Path tempDir;

    private final TextContentExtractor extractor = new TextContentExtractor();

    @Test
    void extractPdfTextWithPdfbox() throws Exception {
        Path pdfPath = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText("WikiForge PDF text");
                contentStream.endText();
            }
            document.save(pdfPath.toFile());
        }

        ParsedTextContent content = extractor.extract(pdfPath, "pdf").orElseThrow();

        assertThat(content.parserName()).isEqualTo("pdfbox-text");
        assertThat(content.parseStatus()).isEqualTo("success");
        assertThat(content.parsedText()).contains("WikiForge PDF text");
        assertThat(content.rawTextSaved()).isTrue();
    }

    @Test
    void extractDocxTextWithPoi() throws Exception {
        Path docxPath = tempDir.resolve("sample.docx");
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("WikiForge Word text");
            document.write(java.nio.file.Files.newOutputStream(docxPath));
        }

        ParsedTextContent content = extractor.extract(docxPath, "docx").orElseThrow();

        assertThat(content.parserName()).isEqualTo("poi-docx-text");
        assertThat(content.parseStatus()).isEqualTo("success");
        assertThat(content.parsedText()).contains("WikiForge Word text");
        assertThat(content.rawTextSaved()).isTrue();
    }
}
