package com.wikiforge.core.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wikiforge.core.application.dto.DictionaryItemResponse;
import com.wikiforge.core.application.dto.DictionaryListResponse;
import com.wikiforge.core.infrastructure.persistence.SystemDictionaryEntity;
import com.wikiforge.core.infrastructure.persistence.SystemDictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemDictionaryService {

    private final SystemDictionaryMapper systemDictionaryMapper;

    public SystemDictionaryService(SystemDictionaryMapper systemDictionaryMapper) {
        this.systemDictionaryMapper = systemDictionaryMapper;
    }

    @Transactional(readOnly = true)
    public DictionaryListResponse listDictionaries(String dictType) {
        LambdaQueryWrapper<SystemDictionaryEntity> wrapper =
                new LambdaQueryWrapper<SystemDictionaryEntity>()
                        .eq(SystemDictionaryEntity::getActive, true)
                        .orderByAsc(SystemDictionaryEntity::getDictType)
                        .orderByAsc(SystemDictionaryEntity::getSortOrder)
                        .orderByAsc(SystemDictionaryEntity::getId);
        if (dictType != null && !dictType.isBlank()) {
            wrapper.eq(SystemDictionaryEntity::getDictType, dictType.trim());
        }
        return new DictionaryListResponse(
                systemDictionaryMapper.selectList(wrapper).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private DictionaryItemResponse toResponse(SystemDictionaryEntity entity) {
        return new DictionaryItemResponse(
                entity.getDictType(),
                entity.getDictCode(),
                entity.getLabelZh(),
                entity.getDescriptionZh(),
                entity.getSortOrder(),
                entity.getColorToken(),
                Boolean.TRUE.equals(entity.getTerminal()),
                Boolean.TRUE.equals(entity.getSuccess())
        );
    }
}
