package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wikiforge.core.domain.model.SourceContent;
import com.wikiforge.core.domain.repository.SourceContentRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSourceContentRepository implements SourceContentRepository {

    private final SourceFileMapper sourceFileMapper;
    private final SourceContentMapper sourceContentMapper;

    public MyBatisSourceContentRepository(
            SourceFileMapper sourceFileMapper,
            SourceContentMapper sourceContentMapper
    ) {
        this.sourceFileMapper = sourceFileMapper;
        this.sourceContentMapper = sourceContentMapper;
    }

    @Override
    public Optional<SourceContent> findBySourceFileUid(String fileUid) {
        SourceFileEntity sourceFile = sourceFileMapper.selectOne(
                new LambdaQueryWrapper<SourceFileEntity>()
                        .eq(SourceFileEntity::getFileUid, fileUid)
        );
        if (sourceFile == null) {
            return Optional.empty();
        }
        SourceContentEntity content = sourceContentMapper.selectOne(
                new LambdaQueryWrapper<SourceContentEntity>()
                        .eq(SourceContentEntity::getSourceFileId, sourceFile.getId())
        );
        return Optional.ofNullable(content).map(this::toModel);
    }

    private SourceContent toModel(SourceContentEntity entity) {
        return new SourceContent(
                entity.getId(),
                entity.getContentUid(),
                entity.getSourceId(),
                entity.getSourceFileId(),
                entity.getParserName(),
                entity.getContentType(),
                entity.getRawText(),
                entity.getTextHash(),
                entity.getCharCount(),
                entity.getRawTextSaved(),
                entity.getParseStatus(),
                entity.getParseError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
