package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wikiforge.core.domain.model.ObsidianNote;
import com.wikiforge.core.domain.repository.ObsidianNoteRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisObsidianNoteRepository implements ObsidianNoteRepository {

    private final ObsidianNoteMapper obsidianNoteMapper;
    private final SourceFileMapper sourceFileMapper;

    public MyBatisObsidianNoteRepository(
            ObsidianNoteMapper obsidianNoteMapper,
            SourceFileMapper sourceFileMapper
    ) {
        this.obsidianNoteMapper = obsidianNoteMapper;
        this.sourceFileMapper = sourceFileMapper;
    }

    @Override
    public ObsidianNote save(ObsidianNote note) {
        ObsidianNoteEntity entity = toEntity(note);
        obsidianNoteMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<ObsidianNote> findByNoteUid(String noteUid) {
        ObsidianNoteEntity entity = obsidianNoteMapper.selectOne(
                new LambdaQueryWrapper<ObsidianNoteEntity>()
                        .eq(ObsidianNoteEntity::getNoteUid, noteUid)
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<ObsidianNote> findBySourceFileUid(String fileUid) {
        SourceFileEntity sourceFile = sourceFileMapper.selectOne(
                new LambdaQueryWrapper<SourceFileEntity>()
                        .eq(SourceFileEntity::getFileUid, fileUid)
        );
        if (sourceFile == null) {
            return Optional.empty();
        }
        ObsidianNoteEntity entity = obsidianNoteMapper.selectOne(
                new LambdaQueryWrapper<ObsidianNoteEntity>()
                        .eq(ObsidianNoteEntity::getSourceFileId, sourceFile.getId())
                        .orderByDesc(ObsidianNoteEntity::getId)
                        .last("LIMIT 1")
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<ObsidianNote> findLatest() {
        ObsidianNoteEntity entity = obsidianNoteMapper.selectOne(
                new LambdaQueryWrapper<ObsidianNoteEntity>()
                        .orderByDesc(ObsidianNoteEntity::getId)
                        .last("LIMIT 1")
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    private ObsidianNoteEntity toEntity(ObsidianNote note) {
        ObsidianNoteEntity entity = new ObsidianNoteEntity();
        entity.setId(note.id());
        entity.setNoteUid(note.noteUid());
        entity.setSourceId(note.sourceId());
        entity.setSourceFileId(note.sourceFileId());
        entity.setNoteType(note.noteType());
        entity.setVaultName(note.vaultName());
        entity.setVaultPath(note.vaultPath());
        entity.setAbsolutePath(note.absolutePath());
        entity.setObsidianUri(note.obsidianUri());
        entity.setTitle(note.title());
        entity.setFrontmatterJson(note.frontmatterJson());
        entity.setContentHash(note.contentHash());
        entity.setStatus(note.status());
        entity.setCreatedAt(note.createdAt());
        entity.setUpdatedAt(note.updatedAt());
        return entity;
    }

    private ObsidianNote toDomain(ObsidianNoteEntity entity) {
        return new ObsidianNote(
                entity.getId(),
                entity.getNoteUid(),
                entity.getSourceId(),
                entity.getSourceFileId(),
                entity.getNoteType(),
                entity.getVaultName(),
                entity.getVaultPath(),
                entity.getAbsolutePath(),
                entity.getObsidianUri(),
                entity.getTitle(),
                entity.getFrontmatterJson(),
                entity.getContentHash(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
