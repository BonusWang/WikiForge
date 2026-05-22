package com.wikiforge.core.domain.model;

import java.util.List;

public record ImportJobPage(List<ImportJob> items, int page, int pageSize, long total) {
}
