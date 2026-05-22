package com.wikiforge.core.infrastructure.integration;

import com.wikiforge.core.application.dto.RunLocalImportJobRequest;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import com.wikiforge.core.application.service.CoreRuntimeProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpWorkerImportJobClient implements WorkerImportJobClient {

    private final RestClient restClient;

    public HttpWorkerImportJobClient(RestClient.Builder restClientBuilder, CoreRuntimeProperties runtimeProperties) {
        this.restClient = restClientBuilder.baseUrl(runtimeProperties.workerBaseUrl()).build();
    }

    @Override
    public void startLocalImportJob(RunLocalImportJobRequest request) {
        restClient.post()
                .uri("/api/v1/worker/import-jobs/local/run")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
