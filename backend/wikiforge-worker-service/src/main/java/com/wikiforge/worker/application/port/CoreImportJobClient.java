package com.wikiforge.worker.application.port;

import com.wikiforge.worker.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.worker.application.dto.UpdateImportJobStatusRequest;

public interface CoreImportJobClient {

    void updateStatus(String jobUid, UpdateImportJobStatusRequest request);

    void submitSourceFilesBatch(String jobUid, SubmitSourceFilesBatchRequest request);
}
