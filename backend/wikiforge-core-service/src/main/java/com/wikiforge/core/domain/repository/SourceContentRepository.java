package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.SourceContent;
import java.util.Optional;

public interface SourceContentRepository {

    Optional<SourceContent> findBySourceFileUid(String fileUid);
}
