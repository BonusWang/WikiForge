package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("review_items")
public class ReviewItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reviewUid;
    private Long sourceId;
    private Long sourceFileId;
    private Long runId;
    private String reviewType;
    private String status;
    private String reason;
    private String suggestedChangesJson;
    private String markdownDraft;
    private String userDecision;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReviewUid() {
        return reviewUid;
    }

    public void setReviewUid(String reviewUid) {
        this.reviewUid = reviewUid;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getSourceFileId() {
        return sourceFileId;
    }

    public void setSourceFileId(Long sourceFileId) {
        this.sourceFileId = sourceFileId;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestedChangesJson() {
        return suggestedChangesJson;
    }

    public void setSuggestedChangesJson(String suggestedChangesJson) {
        this.suggestedChangesJson = suggestedChangesJson;
    }

    public String getMarkdownDraft() {
        return markdownDraft;
    }

    public void setMarkdownDraft(String markdownDraft) {
        this.markdownDraft = markdownDraft;
    }

    public String getUserDecision() {
        return userDecision;
    }

    public void setUserDecision(String userDecision) {
        this.userDecision = userDecision;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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
