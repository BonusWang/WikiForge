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

    @Test
    void vectorExportMigrationCreatesJobAndChunkTables() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_002__create_vector_export_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE vector_export_jobs");
            assertThat(migrationSql).contains("CREATE TABLE content_chunks");
            assertThat(migrationSql).contains("export_relative_path VARCHAR(1024) NULL");
            assertThat(migrationSql).contains("chunk_text LONGTEXT NOT NULL");
            assertThat(migrationSql).contains("embedding_status VARCHAR(64) NOT NULL DEFAULT 'pending'");
            assertThat(migrationSql).contains("KEY idx_content_chunks_target_collection (target_collection)");
        }
    }

    @Test
    void knowledgeMaintenanceMigrationCreatesRunAndItemTables() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_003__create_knowledge_maintenance_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE knowledge_maintenance_runs");
            assertThat(migrationSql).contains("CREATE TABLE knowledge_maintenance_items");
            assertThat(migrationSql).contains("run_uid VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("issue_type VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("evidence_json JSON NULL");
            assertThat(migrationSql).contains("KEY idx_maintenance_items_status_created (status, created_at)");
        }
    }

    @Test
    void knowledgeMaintenanceWorkflowMigrationAddsResolutionColumns() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_004__extend_maintenance_items_workflow.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("ADD COLUMN resolution_note TEXT NULL");
            assertThat(migrationSql).contains("ADD COLUMN resolved_by VARCHAR(128) NULL");
            assertThat(migrationSql).contains("ADD COLUMN resolved_at DATETIME NULL");
            assertThat(migrationSql).contains("KEY idx_maintenance_items_resolved_at (resolved_at)");
        }
    }

    @Test
    void wikiCompileMigrationCreatesPageAndIntegrationTables() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_005__create_wiki_compile_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE wiki_pages");
            assertThat(migrationSql).contains("CREATE TABLE wiki_integrations");
            assertThat(migrationSql).contains("page_type VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("confidence_score DECIMAL(5,4) NOT NULL DEFAULT 0");
            assertThat(migrationSql).contains("KEY idx_wiki_integrations_status_created (status, created_at)");
        }
    }

    @Test
    void mvp0DictionaryAndWikiIngestMigrationCreatesOnlyCurrentTables() throws Exception {
        try (var inputStream = getClass().getResourceAsStream(
                "/db/migration/V20260524_006__create_mvp0_dictionary_and_wiki_ingest_tables.sql"
        )) {
            assertThat(inputStream).isNotNull();
            String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migrationSql).contains("CREATE TABLE system_dictionaries");
            assertThat(migrationSql).contains("CREATE TABLE wiki_ingest_runs");
            assertThat(migrationSql).contains("UNIQUE KEY uk_system_dictionaries_type_code (dict_type, dict_code)");
            assertThat(migrationSql).contains("已收纳");
            assertThat(migrationSql).contains("待整理到 Wiki");
            assertThat(migrationSql).contains("run_uid VARCHAR(64) NOT NULL");
            assertThat(migrationSql).contains("status_code VARCHAR(128) NOT NULL DEFAULT '已创建'");
            assertThat(migrationSql).doesNotContain("CREATE TABLE agent_runs");
            assertThat(migrationSql).doesNotContain("CREATE TABLE vector_export_jobs");
        }
    }
}
