package com.wikiforge.core;

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
}
