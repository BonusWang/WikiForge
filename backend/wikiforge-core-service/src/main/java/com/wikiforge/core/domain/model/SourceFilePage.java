package com.wikiforge.core.domain.model;

import java.util.List;

public record SourceFilePage(List<SourceFileRecord> items, int page, int pageSize, long total) {
}
