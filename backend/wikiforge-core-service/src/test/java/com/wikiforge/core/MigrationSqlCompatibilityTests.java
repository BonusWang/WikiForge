package com.wikiforge.core;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSqlCompatibilityTests {

    @Test
    void sourceImportMigrationAvoidsMysqlReservedRecursiveColumn() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260523_002__create_source_import_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).doesNotContain("\n    recursive TINYINT");
            assertThat(migrationSql).contains("\n    recursive_scan TINYINT");
        }
    }

    @Test
    void obsidianNotesMigrationUsesIndexablePathColumns() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260523_003__create_obsidian_notes.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("vault_path VARCHAR(1024) NOT NULL");
            assertThat(migrationSql).contains("absolute_path VARCHAR(2048) NOT NULL");
            assertThat(migrationSql).contains("obsidian_uri VARCHAR(2048) NOT NULL");
            assertThat(migrationSql).contains("KEY idx_obsidian_notes_vault_path (vault_path(255))");
        }
    }

    @Test
    void sourceContentsMigrationKeepsRawTextOutOfSourcesTable() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260523_004__create_source_contents.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE source_contents");
            assertThat(migrationSql).contains("source_file_id BIGINT NOT NULL");
            assertThat(migrationSql).contains("raw_text LONGTEXT NULL");
            assertThat(migrationSql).contains("UNIQUE KEY uk_source_contents_source_file (source_file_id)");
            assertThat(migrationSql).doesNotContain("ALTER TABLE sources ADD raw_text");
        }
    }

    @Test
    void agentReviewMigrationCreatesRunStepAndReviewTables() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260523_005__create_agent_review_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE agent_runs");
            assertThat(migrationSql).contains("CREATE TABLE agent_steps");
            assertThat(migrationSql).contains("CREATE TABLE review_items");
            assertThat(migrationSql).contains("run_uid VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("review_uid VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("suggested_changes_json JSON NULL");
            assertThat(migrationSql).contains("KEY idx_review_items_status_created (status, created_at)");
        }
    }

    @Test
    void v1PersonalRecordMigrationAddsObsidianArchiveColumns() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_001__extend_personal_records_for_v1.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("ADD COLUMN obsidian_vault_path VARCHAR(1024) NULL");
            assertThat(migrationSql).contains("ADD COLUMN obsidian_uri VARCHAR(2048) NULL");
            assertThat(migrationSql).contains("ADD COLUMN archived_at DATETIME NULL");
            assertThat(migrationSql).contains("idx_personal_records_archived_at");
        }
    }
}
