package com.wikiforge.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSqlCompatibilityTests {

    @Test
    void sourceImportMigrationAvoidsMysqlReservedRecursiveColumn() throws Exception {
        String migrationSql = migrationSql("/db/migration/V20260523_002__create_source_import_tables.sql");
        assertThat(migrationSql).doesNotContain("\n    recursive TINYINT");
        assertThat(migrationSql).contains("\n    recursive_scan TINYINT");
    }

    @Test
    void sourceContentsMigrationUsesSourceFileAsOnlySourceLedger() throws Exception {
        String importMigrationSql = migrationSql("/db/migration/V20260523_002__create_source_import_tables.sql");
        String contentsMigrationSql = migrationSql("/db/migration/V20260523_004__create_source_contents.sql");
        assertThat(importMigrationSql).contains("CREATE TABLE source_files");
        assertThat(importMigrationSql).doesNotContain("CREATE TABLE sources");
        assertThat(importMigrationSql).doesNotContain("source_id BIGINT");
        assertThat(contentsMigrationSql).contains("CREATE TABLE source_contents");
        assertThat(contentsMigrationSql).contains("source_file_id BIGINT NOT NULL");
        assertThat(contentsMigrationSql).contains("raw_text LONGTEXT NULL");
        assertThat(contentsMigrationSql).contains("UNIQUE KEY uk_source_contents_source_file (source_file_id)");
        assertThat(contentsMigrationSql).doesNotContain("source_id BIGINT");
        assertThat(contentsMigrationSql).doesNotContain("REFERENCES sources");
    }

    @Test
    void mvp0DictionaryAndWikiIngestMigrationCreatesOnlyCurrentTables() throws Exception {
        String migrationSql = migrationSql(
                "/db/migration/V20260524_006__create_mvp0_dictionary_and_wiki_ingest_tables.sql"
        );
        assertThat(migrationSql).contains("CREATE TABLE system_dictionaries");
        assertThat(migrationSql).contains("CREATE TABLE wiki_ingest_runs");
        assertThat(migrationSql).contains("UNIQUE KEY uk_system_dictionaries_type_code (dict_type, dict_code)");
        assertThat(migrationSql).contains("已收纳");
        assertThat(migrationSql).contains("待整理到 Wiki");
        assertThat(migrationSql).contains("run_uid VARCHAR(64) NOT NULL");
        assertThat(migrationSql).contains("status_code VARCHAR(128) NOT NULL DEFAULT '已创建'");
        assertThat(migrationSql).doesNotContain("CREATE TABLE obsidian_notes");
        assertThat(migrationSql).doesNotContain("CREATE TABLE agent_runs");
        assertThat(migrationSql).doesNotContain("CREATE TABLE review_items");
        assertThat(migrationSql).doesNotContain("CREATE TABLE mcp_tool_calls");
        assertThat(migrationSql).doesNotContain("CREATE TABLE personal_records");
        assertThat(migrationSql).doesNotContain("CREATE TABLE vector_export_jobs");
        assertThat(migrationSql).doesNotContain("CREATE TABLE content_chunks");
        assertThat(migrationSql).doesNotContain("CREATE TABLE knowledge_maintenance_runs");
        assertThat(migrationSql).doesNotContain("CREATE TABLE wiki_pages");
        assertThat(migrationSql).doesNotContain("CREATE TABLE wiki_integrations");
        assertThat(migrationSql).doesNotContain("CREATE TABLE system_settings");
        assertThat(migrationSql).doesNotContain("CREATE TABLE model_providers");
    }

    @Test
    void retiredCapabilityMigrationsAreRemovedFromMvp0SourceTree() {
        assertThat(migrationSource("V20260523_003__create_obsidian_notes.sql")).doesNotExist();
        assertThat(migrationSource("V20260523_005__create_agent_review_tables.sql")).doesNotExist();
        assertThat(migrationSource("V20260523_006__create_mcp_preview_tables.sql")).doesNotExist();
        assertThat(migrationSource("V20260524_001__extend_personal_records_for_v1.sql")).doesNotExist();
        assertThat(migrationSource("V20260524_002__create_vector_export_tables.sql")).doesNotExist();
        assertThat(migrationSource("V20260524_003__create_knowledge_maintenance_tables.sql")).doesNotExist();
        assertThat(migrationSource("V20260524_004__extend_maintenance_items_workflow.sql")).doesNotExist();
        assertThat(migrationSource("V20260524_005__create_wiki_compile_tables.sql")).doesNotExist();
        assertThat(migrationSource("V20260523_001__create_mvp0_tables.sql")).doesNotExist();
    }

    private String migrationSql(String path) throws Exception {
        try (var inputStream = getClass().getResourceAsStream(path)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path migrationSource(String fileName) {
        return Path.of("src", "main", "resources", "db", "migration", fileName);
    }
}
