package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = WikiForgeCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WikiForgeCoreApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthApiReturnsCoreServiceName() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("wikiforge-core-service");
    }

    @Test
    void versionApiReturnsCurrentSmallRelease() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/version", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("product").asText()).isEqualTo("WikiForge");
        assertThat(data.path("service").asText()).isEqualTo("wikiforge-core-service");
        assertThat(data.path("version").asText()).isEqualTo("2.0-v2-preview.4");
        assertThat(data.path("stage").asText()).isEqualTo("R6-version-api");
        assertThat(data.path("releaseDate").asText()).isEqualTo("2026-05-24");
        assertThat(data.path("apiBasePath").asText()).isEqualTo("/api/v1");
    }
}
