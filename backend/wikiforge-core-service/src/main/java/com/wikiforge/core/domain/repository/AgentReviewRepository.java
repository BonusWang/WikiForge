package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.AgentRun;
import com.wikiforge.core.domain.model.AgentStep;
import com.wikiforge.core.domain.model.ReviewItem;
import com.wikiforge.core.domain.model.ReviewItemPage;
import java.util.Optional;

public interface AgentReviewRepository {

    AgentRun saveRun(AgentRun run);

    void saveStep(AgentStep step);

    ReviewItem saveReviewItem(ReviewItem reviewItem);

    Optional<AgentRun> findRunByRunUid(String runUid);

    Optional<ReviewItem> findLatestReviewItemByRunId(Long runId);

    ReviewItemPage findReviewItems(String status, int page, int pageSize);
}
