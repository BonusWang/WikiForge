package com.wikiforge.worker.infrastructure.integration;

import com.wikiforge.worker.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.worker.application.dto.UpdateImportJobStatusRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpCoreImportJobClientTests {

    @Test
    void callbacksSendInternalTokenHeader() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        HttpCoreImportJobClient client = new HttpCoreImportJobClient(
                restClientBuilder,
                "http://core.test",
                "secret-token"
        );

        server.expect(requestTo("http://core.test/api/v1/internal/import-jobs/job_1/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-WikiForge-Internal-Token", "secret-token"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://core.test/api/v1/internal/import-jobs/job_1/source-files/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-WikiForge-Internal-Token", "secret-token"))
                .andRespond(withSuccess());

        client.updateStatus("job_1", new UpdateImportJobStatusRequest("running", 0, 0, 0, 0, null));
        client.submitSourceFilesBatch("job_1", new SubmitSourceFilesBatchRequest(List.of()));

        server.verify();
    }
}
