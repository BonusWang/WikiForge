ALTER TABLE personal_records
    ADD COLUMN obsidian_vault_path VARCHAR(1024) NULL,
    ADD COLUMN obsidian_uri VARCHAR(2048) NULL,
    ADD COLUMN archived_at DATETIME NULL;

CREATE INDEX idx_personal_records_archived_at ON personal_records (archived_at);
