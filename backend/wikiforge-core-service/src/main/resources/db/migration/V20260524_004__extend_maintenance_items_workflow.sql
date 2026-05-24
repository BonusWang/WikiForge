ALTER TABLE knowledge_maintenance_items
    ADD COLUMN resolution_note TEXT NULL AFTER status,
    ADD COLUMN resolved_by VARCHAR(128) NULL AFTER resolution_note,
    ADD COLUMN resolved_at DATETIME NULL AFTER resolved_by,
    ADD KEY idx_maintenance_items_resolved_at (resolved_at);
