package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.ObsidianNote;
import java.util.Optional;

public interface ObsidianNoteRepository {

    ObsidianNote save(ObsidianNote note);

    Optional<ObsidianNote> findByNoteUid(String noteUid);

    Optional<ObsidianNote> findBySourceFileUid(String fileUid);
}
