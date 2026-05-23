package com.wikiforge.worker.infrastructure.integration;

import com.wikiforge.worker.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.worker.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.worker.application.port.CoreImportJobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpCoreImportJobClient implements CoreImportJobClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-WikiForge-Internal-Token";

    private final RestClient restClient;
    private final String internalApiToken;

    @Autowired
    public HttpCoreImportJobClient(
            RestClient.Builder restClientBuilder,
            @Value("${WIKIFORGE_CORE_SERVICE_BASE_URL:${wikiforge.core-service.base-url:http://localhost:8080}}") String coreServiceBaseUrl,
            @Value("${WIKIFORGE_INTERNAL_API_TOKEN:${wikiforge.internal-api-token:}}") String internalApiToken
    ) {
        this.restClient = restClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl(coreServiceBaseUrl)
                .build();
        this.internalApiToken = internalApiToken;
    }

    HttpCoreImportJobClient(RestClient restClient, String internalApiToken) {
        this.restClient = restClient;
        this.internalApiToken = internalApiToken;
    }

    @Override
    public void updateStatus(String jobUid, UpdateImportJobStatusRequest request) {
        restClient.patch()
                .uri("/api/v1/internal/import-jobs/{jobUid}/status", jobUid)
                .header(INTERNAL_TOKEN_HEADER, internalApiToken)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void submitSourceFilesBatch(String jobUid, SubmitSourceFilesBatchRequest request) {
        restClient.post()
                .uri("/api/v1/internal/import-jobs/{jobUid}/source-files/batch", jobUid)
                .header(INTERNAL_TOKEN_HEADER, internalApiToken)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
