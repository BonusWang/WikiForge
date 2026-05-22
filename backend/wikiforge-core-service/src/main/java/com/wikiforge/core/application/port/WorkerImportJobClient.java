package com.wikiforge.core.application.port;

import com.wikiforge.core.application.dto.RunLocalImportJobRequest;

public interface WorkerImportJobClient {

    void startLocalImportJob(RunLocalImportJobRequest request);
}
