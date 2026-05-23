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
}
