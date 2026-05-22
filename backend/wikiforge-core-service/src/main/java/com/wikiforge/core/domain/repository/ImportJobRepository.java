package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.ImportJobPage;
import java.util.Optional;

public interface ImportJobRepository {

    void save(ImportJob importJob);

    void update(ImportJob importJob);

    Optional<ImportJob> findByJobUid(String jobUid);

    ImportJobPage findPage(String status, int page, int pageSize);
}
