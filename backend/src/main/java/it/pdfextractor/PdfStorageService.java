package it.pdfextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PdfStorageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private final Path uploadDirectory;

    public PdfStorageService(@Value("${app.upload-dir}") String uploadDirectory) throws IOException {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDirectory);
    }

    public StoredFile store(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.length() <= 4
                || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato non valido. Seleziona un file con estensione .pdf.");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file PDF e' vuoto.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Il file supera il limite di 10 MB.");
        }

        byte[] content = file.getBytes();
        validatePdf(content);

        String id = UUID.randomUUID() + ".pdf";
        Path destination = uploadDirectory.resolve(id);
        Path temporaryFile = Files.createTempFile(uploadDirectory, ".upload-", ".tmp");
        try {
            Files.write(temporaryFile, content);
            Files.move(temporaryFile, destination);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
        return new StoredFile(id, content.length);
    }

    private void validatePdf(byte[] content) {
        if (content.length < 5 || content[0] != '%' || content[1] != 'P'
                || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
            throw invalidPdf();
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0) {
                throw invalidPdf();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidPdf();
        }
    }

    private ResponseStatusException invalidPdf() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Il contenuto non e' un PDF valido, leggibile e non protetto.");
    }

    public record StoredFile(String id, long size) { }
}