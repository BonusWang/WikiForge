package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.SourceFilePage;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.model.SourceFileSubmission;
import java.util.List;
import java.util.Optional;

public interface SourceFileRepository {

    void saveAll(ImportJob importJob, List<SourceFileSubmission> files);

    SourceFilePage findByJob(ImportJob importJob, int page, int pageSize);

    Optional<SourceFileRecord> findByFileUid(String fileUid);
}
