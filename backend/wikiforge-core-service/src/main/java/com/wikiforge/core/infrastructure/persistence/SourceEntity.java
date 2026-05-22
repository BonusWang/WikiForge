package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sources")
public class SourceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceUid;
    private String title;
    private String sourceType;
    private String sourcePlatform;
    private String rawOriginalPath;
    private String rawManagedPath;
    private String rawOrganizeStatus;
    private String contentHash;
    private String status;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceUid() {
        return sourceUid;
    }

    public void setSourceUid(String sourceUid) {
        this.sourceUid = sourceUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourcePlatform() {
        return sourcePlatform;
    }

    public void setSourcePlatform(String sourcePlatform) {
        this.sourcePlatform = sourcePlatform;
    }

    public String getRawOriginalPath() {
        return rawOriginalPath;
    }

    public void setRawOriginalPath(String rawOriginalPath) {
        this.rawOriginalPath = rawOriginalPath;
    }

    public String getRawManagedPath() {
        return rawManagedPath;
    }

    public void setRawManagedPath(String rawManagedPath) {
        this.rawManagedPath = rawManagedPath;
    }

    public String getRawOrganizeStatus() {
        return rawOrganizeStatus;
    }

    public void setRawOrganizeStatus(String rawOrganizeStatus) {
        this.rawOrganizeStatus = rawOrganizeStatus;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
