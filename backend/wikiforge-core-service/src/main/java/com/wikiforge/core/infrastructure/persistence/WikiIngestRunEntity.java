package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wiki_ingest_runs")
public class WikiIngestRunEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String runUid;
    private Long sourceFileId;
    private String fileUid;
    private String fileName;
    private String statusCode;
    private String statusLabel;
    private String sourcePagePath;
    private String wikiPagePaths;
    private Boolean indexUpdated;
    private Boolean logEntryAppended;
    private String writeStatusCode;
    private String writeStatusLabel;
    private String fallbackReason;
    private String failureReason;
    private String managedBlockPreview;
    private String logEntryPreview;
    private String obsidianUri;
    private Boolean retryable;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunUid() {
        return runUid;
    }

    public void setRunUid(String runUid) {
        this.runUid = runUid;
    }

    public Long getSourceFileId() {
        return sourceFileId;
    }

    public void setSourceFileId(Long sourceFileId) {
        this.sourceFileId = sourceFileId;
    }

    public String getFileUid() {
        return fileUid;
    }

    public void setFileUid(String fileUid) {
        this.fileUid = fileUid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getSourcePagePath() {
        return sourcePagePath;
    }

    public void setSourcePagePath(String sourcePagePath) {
        this.sourcePagePath = sourcePagePath;
    }

    public String getWikiPagePaths() {
        return wikiPagePaths;
    }

    public void setWikiPagePaths(String wikiPagePaths) {
        this.wikiPagePaths = wikiPagePaths;
    }

    public Boolean getIndexUpdated() {
        return indexUpdated;
    }

    public void setIndexUpdated(Boolean indexUpdated) {
        this.indexUpdated = indexUpdated;
    }

    public Boolean getLogEntryAppended() {
        return logEntryAppended;
    }

    public void setLogEntryAppended(Boolean logEntryAppended) {
        this.logEntryAppended = logEntryAppended;
    }

    public String getWriteStatusCode() {
        return writeStatusCode;
    }

    public void setWriteStatusCode(String writeStatusCode) {
        this.writeStatusCode = writeStatusCode;
    }

    public String getWriteStatusLabel() {
        return writeStatusLabel;
    }

    public void setWriteStatusLabel(String writeStatusLabel) {
        this.writeStatusLabel = writeStatusLabel;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getManagedBlockPreview() {
        return managedBlockPreview;
    }

    public void setManagedBlockPreview(String managedBlockPreview) {
        this.managedBlockPreview = managedBlockPreview;
    }

    public String getLogEntryPreview() {
        return logEntryPreview;
    }

    public void setLogEntryPreview(String logEntryPreview) {
        this.logEntryPreview = logEntryPreview;
    }

    public String getObsidianUri() {
        return obsidianUri;
    }

    public void setObsidianUri(String obsidianUri) {
        this.obsidianUri = obsidianUri;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
