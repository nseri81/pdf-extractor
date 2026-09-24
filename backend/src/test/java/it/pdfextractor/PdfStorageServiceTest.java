package it.pdfextractor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfStorageServiceTest {
    @TempDir
    Path directory;

    @Test
    void storesOriginalBytesWithUniqueNames() throws Exception {
        PdfStorageService service = new PdfStorageService(directory.toString());
        byte[] content = pdf(false);
        MockMultipartFile file = new MockMultipartFile("file", "DOCUMENTO.PDF", "", content);
        var first = service.store(file);
        var second = service.store(file);
        assertThat(first.id()).matches("[0-9a-f-]{36}\\.pdf").isNotEqualTo(second.id());
        assertThat(first.size()).isEqualTo(content.length);
        assertThat(Files.readAllBytes(directory.resolve(first.id()))).isEqualTo(content);
        assertThat(Files.readAllBytes(directory.resolve(second.id()))).isEqualTo(content);
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).hasSize(2);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"foto.png", "documento.pdf.exe", "documento", ".pdf", "../documento.pdf", "..\\documento.pdf"})
    void rejectsWrongOrUnsafeNamesWithoutSaving(String filename) throws Exception {
        assertRejected(filename, pdf(false), 400);
    }

    @Test
    void rejectsRenamedTextDespitePdfMime() throws Exception {
        assertRejected("falso.pdf", "not a PDF".getBytes(StandardCharsets.UTF_8), 400);
    }

    @Test
    void rejectsForgedHeader() throws Exception {
        assertRejected("falso.pdf", "%PDF-1.7\nnot a document".getBytes(StandardCharsets.UTF_8), 400);
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        assertRejected("vuoto.pdf", new byte[0], 400);
    }

    @Test
    void rejectsLargeFile() throws Exception {
        assertRejected("grande.pdf", new byte[10 * 1024 * 1024 + 1], 413);
    }

    @Test
    void rejectsEncryptedPdf() throws Exception {
        assertRejected("protetto.pdf", pdf(true), 400);
    }

    @Test
    void rejectsPdfWithoutPages() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.save(output);
            assertRejected("vuoto.pdf", output.toByteArray(), 400);
        }
    }

    private void assertRejected(String filename, byte[] content, int status) throws Exception {
        PdfStorageService service = new PdfStorageService(directory.toString());
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", filename, "application/pdf", content)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).isEmpty();
        }
    }

    static byte[] pdf(boolean encrypted) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            if (encrypted) {
                document.protect(new StandardProtectionPolicy("owner", "reader", new AccessPermission()));
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}