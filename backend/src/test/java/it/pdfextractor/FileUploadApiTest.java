package it.pdfextractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadApiTest {
    @TempDir
    static Path directory;

    @Autowired
    TestRestTemplate client;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.upload-dir", () -> directory.toString());
    }

    @BeforeAll
    static void browserFixture() throws Exception {
        Path fixtures = Path.of("target", "test-fixtures");
        Files.createDirectories(fixtures);
        Files.write(fixtures.resolve("valid.pdf"), PdfStorageServiceTest.pdf(false));
    }

    @Test
    void uploadsAndPersistsExactBytesWithoutOverwritingOrExposingFiles() throws Exception {
        byte[] content = PdfStorageServiceTest.pdf(false);
        ResponseEntity<Map> first = upload("documento.PDF", content);
        ResponseEntity<Map> second = upload("documento.PDF", content);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String firstId = first.getBody().get("id").toString();
        String secondId = second.getBody().get("id").toString();
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(first.getBody()).doesNotContainKeys("path", "directory");
        assertThat(Files.readAllBytes(directory.resolve(firstId))).isEqualTo(content);
        assertThat(Files.readAllBytes(directory.resolve(secondId))).isEqualTo(content);
        assertThat(client.getForEntity("/uploads/" + firstId, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(client.getForEntity("/" + firstId, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidContentAndExtensionWithoutSaving() throws Exception {
        long before = fileCount();
        assertError(upload("foto.png", PdfStorageServiceTest.pdf(false)), HttpStatus.BAD_REQUEST);
        assertError(upload("falso.pdf", "not a PDF".getBytes()), HttpStatus.BAD_REQUEST);
        assertError(upload("falso.pdf", "%PDF-1.7\ninvalid".getBytes()), HttpStatus.BAD_REQUEST);
        assertError(upload("vuoto.pdf", new byte[0]), HttpStatus.BAD_REQUEST);
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void enforcesMultipartSizeLimitOverHttp() throws Exception {
        long before = fileCount();
        assertError(upload("grande.pdf", new byte[10 * 1024 * 1024 + 1]), HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void rejectsMissingFileAndWrongRequestMediaType() throws Exception {
        long before = fileCount();
        HttpHeaders multipartHeaders = new HttpHeaders();
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("other", "value");
        assertError(client.postForEntity("/api/files", new HttpEntity<>(body, multipartHeaders), Map.class),
                HttpStatus.BAD_REQUEST);
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        assertError(client.postForEntity("/api/files", new HttpEntity<>("{}", jsonHeaders), Map.class),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(fileCount()).isEqualTo(before);
    }

    @Test
    void servesFrontendWithoutPublishingSourceOrStorage() {
        assertThat(client.getForObject("/", String.class)).contains("Seleziona file", "./src/app.js");
        assertThat(client.getForObject("/src/app.js", String.class)).contains("/api/files");
        assertThat(client.getForEntity("/assets/upload.svg", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.getForEntity("/backend/pom.xml", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(client.getForEntity("/tests/index.html", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map> upload(String filename, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return client.postForEntity("/api/files", new HttpEntity<>(body, headers), Map.class);
    }

    private void assertError(ResponseEntity<Map> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().get("message")).isInstanceOf(String.class);
    }

    private long fileCount() throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }
}