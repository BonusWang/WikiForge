package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = WikiForgeCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpPreviewApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-mcp-preview-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path OBSIDIAN_VAULT = TEST_ROOT.resolve("WikiForgeVault").normalize();
    private static final String NOTE_VAULT_PATH = "00_Inbox_收集箱/Sources_来源/example.md";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WorkerImportJobClient workerImportJobClient;

    @DynamicPropertySource
    static void coreProperties(DynamicPropertyRegistry registry) {
        registry.add("wikiforge.obsidian-vault-path", OBSIDIAN_VAULT::toString);
        registry.add("wikiforge.obsidian-vault-name", () -> "WikiForgeVault");
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        deleteDirectory(OBSIDIAN_VAULT);
        Files.createDirectories(OBSIDIAN_VAULT.resolve("00_Inbox_收集箱/Sources_来源"));
        Files.writeString(OBSIDIAN_VAULT.resolve(NOTE_VAULT_PATH), "# example note\n\nMCP note markdown");
        jdbcTemplate.execute("DROP TABLE IF EXISTS personal_records");
        jdbcTemplate.execute("DROP TABLE IF EXISTS mcp_tool_calls");
        jdbcTemplate.execute("DROP TABLE IF EXISTS obsidian_notes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_contents");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_files");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sources");
        jdbcTemplate.execute("DROP TABLE IF EXISTS import_jobs");
        jdbcTemplate.execute("""
                CREATE TABLE import_jobs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    job_uid VARCHAR(64) NOT NULL,
                    import_type VARCHAR(64) NOT NULL,
                    input_path CLOB NULL,
                    input_url CLOB NULL,
                    raw_sources_root CLOB NULL,
                    recursive_scan BOOLEAN NOT NULL DEFAULT TRUE,
                    organize_mode VARCHAR(64) NOT NULL DEFAULT 'copy',
                    max_copy_file_size_mb INT NOT NULL DEFAULT 100,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    total_count INT NOT NULL DEFAULT 0,
                    success_count INT NOT NULL DEFAULT 0,
                    skipped_count INT NOT NULL DEFAULT 0,
                    failed_count INT NOT NULL DEFAULT 0,
                    error_message CLOB NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_import_jobs_job_uid (job_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sources (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    source_uid VARCHAR(64) NOT NULL,
                    title VARCHAR(512) NULL,
                    source_type VARCHAR(64) NOT NULL DEFAULT 'file',
                    source_platform VARCHAR(128) NOT NULL DEFAULT 'local',
                    source_url CLOB NULL,
                    connector_name VARCHAR(128) NULL,
                    connector_status VARCHAR(64) NULL,
                    connector_trace_id VARCHAR(128) NULL,
                    local_path CLOB NULL,
                    raw_original_path CLOB NULL,
                    raw_managed_path CLOB NULL,
                    raw_organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    processing_intent VARCHAR(64) NOT NULL DEFAULT 'organize_only',
                    content_hash VARCHAR(128) NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    collected_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_sources_source_uid (source_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE source_files (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    file_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NULL,
                    import_job_id BIGINT NOT NULL,
                    file_name VARCHAR(512) NOT NULL,
                    file_ext VARCHAR(32) NULL,
                    original_path CLOB NOT NULL,
                    managed_path CLOB NULL,
                    file_size BIGINT NOT NULL DEFAULT 0,
                    mime_type VARCHAR(128) NULL,
                    content_hash VARCHAR(128) NULL,
                    parser_name VARCHAR(128) NULL,
                    parse_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    duplicate_of_file_id BIGINT NULL,
                    parse_error CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_files_file_uid (file_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE source_contents (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    content_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NOT NULL,
                    parser_name VARCHAR(128) NULL,
                    content_type VARCHAR(64) NOT NULL DEFAULT 'plain_text',
                    raw_text CLOB NULL,
                    text_hash VARCHAR(128) NULL,
                    char_count INT NOT NULL DEFAULT 0,
                    raw_text_saved BOOLEAN NOT NULL DEFAULT FALSE,
                    parse_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    parse_error CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_contents_content_uid (content_uid),
                    UNIQUE KEY uk_source_contents_source_file (source_file_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE obsidian_notes (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    note_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    note_type VARCHAR(64) NOT NULL DEFAULT 'source_note',
                    vault_name VARCHAR(128) NOT NULL,
                    vault_path VARCHAR(1024) NOT NULL,
                    absolute_path CLOB NOT NULL,
                    obsidian_uri CLOB NOT NULL,
                    title VARCHAR(512) NOT NULL,
                    frontmatter_json CLOB NULL,
                    content_hash VARCHAR(128) NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'written',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_obsidian_notes_note_uid (note_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE mcp_tool_calls (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    call_uid VARCHAR(64) NOT NULL,
                    tool_name VARCHAR(128) NOT NULL,
                    caller_type VARCHAR(64) NOT NULL,
                    caller_id VARCHAR(128) NOT NULL,
                    input_json CLOB NULL,
                    output_json CLOB NULL,
                    status VARCHAR(64) NOT NULL,
                    error_code VARCHAR(64) NULL,
                    error_message CLOB NULL,
                    duration_ms BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mcp_tool_calls_call_uid (call_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE personal_records (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    record_uid VARCHAR(64) NOT NULL,
                    record_type VARCHAR(64) NOT NULL,
                    title VARCHAR(512) NOT NULL,
                    occurred_at TIMESTAMP NULL,
                    source_channel VARCHAR(128) NOT NULL DEFAULT 'mcp',
                    source_ref CLOB NULL,
                    raw_content CLOB NOT NULL,
                    structured_json CLOB NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    sensitivity_level VARCHAR(32) NOT NULL DEFAULT 'medium',
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_personal_records_record_uid (record_uid)
                )
                """);
        seedSource();
    }

    @Test
    void listToolsExposesFrozenMcpPreviewTools() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/mcp/tools", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode tools = response.getBody().path("data").path("tools");
        assertThat(toNames(tools)).containsExactly(
                "search_sources",
                "get_source",
                "create_source",
                "get_obsidian_note",
                "create_personal_record"
        );
        assertThat(tools.get(0).path("inputSchema").path("type").asText()).isEqualTo("object");
        assertThat(tools.get(0).path("enabled").asBoolean()).isTrue();
        assertThat(toTextList(tools.get(2).path("inputSchema").path("required")))
                .contains("title", "rawContent");
        assertThat(tools.get(3).path("enabled").asBoolean()).isTrue();
        assertThat(tools.get(4).path("enabled").asBoolean()).isTrue();
    }

    @Test
    void sourceToolsReturnSanitizedDataAndWriteCallLogs() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-WikiForge-Caller-Type", "agent");
        headers.add("X-WikiForge-Caller-Id", "openclaw");

        ResponseEntity<JsonNode> searchResponse = restTemplate.postForEntity(
                "/api/v1/mcp/tools/search_sources/call",
                new HttpEntity<>(Map.of("arguments", Map.of("keyword", "example", "pageSize", 10)), headers),
                JsonNode.class
        );

        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode searchData = searchResponse.getBody().path("data");
        assertThat(searchData.path("status").asText()).isEqualTo("completed");
        JsonNode firstItem = searchData.path("result").path("items").get(0);
        assertThat(firstItem.path("sourceUid").asText()).isEqualTo("src_test");
        assertThat(firstItem.path("fileUid").asText()).isEqualTo("file_test");
        assertThat(firstItem.path("obsidianNoteUid").asText()).isEqualTo("note_test");
        assertThat(searchData.toString()).doesNotContain("E:/private/input/example.pdf");
        assertThat(searchData.toString()).doesNotContain("E:/wikiforge/raw/example.pdf");

        ResponseEntity<JsonNode> getResponse = restTemplate.postForEntity(
                "/api/v1/mcp/tools/get_source/call",
                new HttpEntity<>(Map.of("arguments", Map.of("fileUid", "file_test")), headers),
                JsonNode.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode getResult = getResponse.getBody().path("data").path("result");
        assertThat(getResult.path("source").path("sourceUid").asText()).isEqualTo("src_test");
        assertThat(getResult.path("content").path("excerpt").asText()).contains("MCP 测试正文");
        assertThat(getResult.path("obsidianNote").path("vaultPath").asText())
                .isEqualTo(NOTE_VAULT_PATH);
        assertThat(getResult.toString()).doesNotContain("E:/private/input/example.pdf");
        assertThat(getResult.toString()).doesNotContain(OBSIDIAN_VAULT.toString());

        ResponseEntity<JsonNode> createResponse = restTemplate.postForEntity(
                "/api/v1/mcp/tools/create_source/call",
                new HttpEntity<>(Map.of("arguments", Map.of(
                        "title", "Manual MCP Note",
                        "rawContent", "private raw content should not be logged",
                        "sourceType", "text"
                )), headers),
                JsonNode.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = createResponse.getBody().path("data").path("result");
        assertThat(created.path("sourceUid").asText()).startsWith("src_");
        assertThat(created.path("status").asText()).isEqualTo("pending");

        Integer callCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mcp_tool_calls", Integer.class);
        assertThat(callCount).isEqualTo(3);
        Integer openClawCalls = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_tool_calls WHERE caller_type = 'agent' AND caller_id = 'openclaw'",
                Integer.class
        );
        assertThat(openClawCalls).isEqualTo(3);
        String createSourceLog = jdbcTemplate.queryForObject(
                "SELECT input_json FROM mcp_tool_calls WHERE tool_name = 'create_source'",
                String.class
        );
        assertThat(createSourceLog).doesNotContain("private raw content should not be logged");
        assertThat(createSourceLog).contains("rawContentLength");
    }

    @Test
    void listCallsReturnsFilteredAuditPageWithoutInputOrOutputPayloads() {
        for (int i = 0; i < 55; i++) {
            insertMcpCallLog(
                    "mcp_call_completed_" + i,
                    "search_sources",
                    "agent",
                    "openclaw",
                    "completed",
                    null,
                    null,
                    "{\"rawContent\":\"private raw " + i + "\"}",
                    "{\"markdown\":\"private markdown " + i + "\"}"
            );
        }
        insertMcpCallLog(
                "mcp_call_failed_1",
                "get_obsidian_note",
                "agent",
                "hermes",
                "failed",
                "MCP_005",
                "Obsidian note not found",
                "{\"noteUid\":\"note_missing\"}",
                "{}"
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/mcp/calls?callerType=agent&status=completed&page=1&pageSize=100",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("pageSize").asInt()).isEqualTo(100);
        assertThat(data.path("total").asLong()).isEqualTo(55);
        JsonNode items = data.path("items");
        assertThat(items).hasSize(55);
        assertThat(items.get(0).path("callUid").asText()).isEqualTo("mcp_call_completed_54");
        assertThat(items.get(0).path("toolName").asText()).isEqualTo("search_sources");
        assertThat(items.get(0).path("callerType").asText()).isEqualTo("agent");
        assertThat(items.get(0).path("callerId").asText()).isEqualTo("openclaw");
        assertThat(items.get(0).path("status").asText()).isEqualTo("completed");
        assertThat(items.get(0).has("inputJson")).isFalse();
        assertThat(items.get(0).has("outputJson")).isFalse();
        assertThat(data.toString()).doesNotContain("private raw");
        assertThat(data.toString()).doesNotContain("private markdown");
        assertThat(data.toString()).doesNotContain("mcp_call_failed_1");
    }

    @Test
    void missingObsidianNoteReturnsContractErrorAndWritesFailedCallLog() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-WikiForge-Caller-Type", "agent");
        headers.add("X-WikiForge-Caller-Id", "hermes");

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/mcp/tools/get_obsidian_note/call",
                new HttpEntity<>(Map.of("arguments", Map.of("noteUid", "note_missing")), headers),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("MCP_005");
        Integer failedCallCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_tool_calls WHERE tool_name = 'get_obsidian_note' "
                        + "AND status = 'failed' AND error_code = 'MCP_005' "
                        + "AND caller_type = 'agent' AND caller_id = 'hermes'",
                Integer.class
        );
        assertThat(failedCallCount).isEqualTo(1);
    }

    @Test
    void obsidianAndPersonalRecordToolsReturnSafeDataAndRedactLogs() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-WikiForge-Caller-Type", "agent");
        headers.add("X-WikiForge-Caller-Id", "hermes");

        ResponseEntity<JsonNode> noteResponse = restTemplate.postForEntity(
                "/api/v1/mcp/tools/get_obsidian_note/call",
                new HttpEntity<>(Map.of("arguments", Map.of("noteUid", "note_test", "includeMarkdown", true)), headers),
                JsonNode.class
        );

        assertThat(noteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode note = noteResponse.getBody().path("data").path("result");
        assertThat(note.path("noteUid").asText()).isEqualTo("note_test");
        assertThat(note.path("sourceUid").asText()).isEqualTo("src_test");
        assertThat(note.path("fileUid").asText()).isEqualTo("file_test");
        assertThat(note.path("vaultPath").asText()).isEqualTo(NOTE_VAULT_PATH);
        assertThat(note.path("markdown").asText()).contains("MCP note markdown");
        assertThat(note.toString()).doesNotContain(OBSIDIAN_VAULT.toString());

        ResponseEntity<JsonNode> recordResponse = restTemplate.postForEntity(
                "/api/v1/mcp/tools/create_personal_record/call",
                new HttpEntity<>(Map.of("arguments", Map.of(
                        "recordType", "expense",
                        "title", "Coffee Bill",
                        "rawContent", "sensitive personal expense detail",
                        "structured", Map.of("amount", 18.5, "currency", "CNY"),
                        "sourceChannel", "hermes",
                        "sourceRef", "chat-123",
                        "sensitivityLevel", "high"
                )), headers),
                JsonNode.class
        );

        assertThat(recordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode record = recordResponse.getBody().path("data").path("result");
        assertThat(record.path("recordUid").asText()).startsWith("record_");
        assertThat(record.path("recordType").asText()).isEqualTo("expense");
        assertThat(record.path("status").asText()).isEqualTo("pending");

        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_records WHERE record_uid = ? AND raw_content = ?",
                Integer.class,
                record.path("recordUid").asText(),
                "sensitive personal expense detail"
        );
        assertThat(recordCount).isEqualTo(1);
        String recordLog = jdbcTemplate.queryForObject(
                "SELECT input_json FROM mcp_tool_calls WHERE tool_name = 'create_personal_record'",
                String.class
        );
        assertThat(recordLog).doesNotContain("sensitive personal expense detail");
        assertThat(recordLog).doesNotContain("amount");
        assertThat(recordLog).contains("rawContentLength");
        assertThat(recordLog).contains("structuredRedacted");
        String noteLog = jdbcTemplate.queryForObject(
                "SELECT output_json FROM mcp_tool_calls WHERE tool_name = 'get_obsidian_note'",
                String.class
        );
        assertThat(noteLog).doesNotContain("MCP note markdown");
        assertThat(noteLog).contains("markdownLength");
    }

    @Test
    void obsidianNoteAbsoluteVaultPathIsBlockedEvenWithoutMarkdown() {
        jdbcTemplate.update(
                "UPDATE obsidian_notes SET vault_path = ? WHERE note_uid = 'note_test'",
                OBSIDIAN_VAULT.resolve(NOTE_VAULT_PATH).toString()
        );

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/mcp/tools/get_obsidian_note/call",
                Map.of("arguments", Map.of("noteUid", "note_test", "includeMarkdown", false)),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("MCP_006");
        assertThat(response.getBody().toString()).doesNotContain(OBSIDIAN_VAULT.toString());
    }

    @Test
    void obsidianNoteSymlinkEscapeIsBlockedBeforeReading() throws Exception {
        Path outsideFile = TEST_ROOT.resolve("outside-secret.md");
        Path linkPath = OBSIDIAN_VAULT.resolve("00_Inbox_收集箱/Sources_来源/escape.md");
        Files.writeString(outsideFile, "outside vault secret");
        try {
            Files.deleteIfExists(linkPath);
            Files.createSymbolicLink(linkPath, outsideFile);
        } catch (Exception exception) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE obsidian_notes SET vault_path = ? WHERE note_uid = 'note_test'",
                "00_Inbox_收集箱/Sources_来源/escape.md"
        );

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/mcp/tools/get_obsidian_note/call",
                Map.of("arguments", Map.of("noteUid", "note_test", "includeMarkdown", true)),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("MCP_006");
        assertThat(response.getBody().toString()).doesNotContain("outside vault secret");
    }

    @Test
    void invalidPersonalRecordTypeReturnsContractError() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/mcp/tools/create_personal_record/call",
                Map.of("arguments", Map.of(
                        "recordType", "unknown",
                        "title", "bad record",
                        "rawContent", "bad record content"
                )),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("RECORD_001");
    }

    private void insertMcpCallLog(
            String callUid,
            String toolName,
            String callerType,
            String callerId,
            String status,
            String errorCode,
            String errorMessage,
            String inputJson,
            String outputJson
    ) {
        jdbcTemplate.update("""
                INSERT INTO mcp_tool_calls (
                    call_uid, tool_name, caller_type, caller_id, input_json, output_json,
                    status, error_code, error_message, duration_ms, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 12, CURRENT_TIMESTAMP)
                """,
                callUid,
                toolName,
                callerType,
                callerId,
                inputJson,
                outputJson,
                status,
                errorCode,
                errorMessage
        );
    }

    private List<String> toNames(JsonNode tools) {
        return tools.findValuesAsText("name");
    }

    private List<String> toTextList(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        arrayNode.forEach(value -> values.add(value.asText()));
        return values;
    }

    private void seedSource() {
        jdbcTemplate.update("""
                INSERT INTO import_jobs (
                    id, job_uid, import_type, input_path, raw_sources_root, status, created_at, updated_at
                ) VALUES (
                    10, 'job_test', 'path_scan', 'E:/private/input', 'E:/wikiforge/raw', 'completed',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO sources (
                    id, source_uid, title, source_type, source_platform, raw_original_path,
                    raw_managed_path, raw_organize_status, content_hash, status, collected_at,
                    created_at, updated_at
                ) VALUES (
                    100, 'src_test', 'example.pdf', 'pdf', 'local', 'E:/private/input/example.pdf',
                    'E:/wikiforge/raw/example.pdf', 'copied', 'hash-test', 'organized', CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    id, file_uid, source_id, import_job_id, file_name, file_ext, original_path,
                    managed_path, file_size, mime_type, content_hash, parser_name, parse_status,
                    organize_status, created_at
                ) VALUES (
                    200, 'file_test', 100, 10, 'example.pdf', 'pdf', 'E:/private/input/example.pdf',
                    'E:/wikiforge/raw/example.pdf', 1234, 'application/pdf', 'hash-test',
                    'pdfbox', 'success', 'copied', CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    id, content_uid, source_id, source_file_id, parser_name, content_type,
                    raw_text, text_hash, char_count, raw_text_saved, parse_status,
                    created_at, updated_at
                ) VALUES (
                    300, 'content_test', 100, 200, 'pdfbox', 'plain_text',
                    '这是一段 MCP 测试正文，用来验证 get_source 只返回安全摘录。', 'text-hash',
                    32, TRUE, 'success', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO obsidian_notes (
                    id, note_uid, source_id, source_file_id, vault_name, vault_path, absolute_path,
                    obsidian_uri, title, status, created_at, updated_at
                ) VALUES (
                    400, 'note_test', 100, 200, 'WikiForgeVault',
                    ?,
                    ?,
                    'obsidian://open?vault=WikiForgeVault&file=00_Inbox', 'example note',
                    'written', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, NOTE_VAULT_PATH, OBSIDIAN_VAULT.resolve(NOTE_VAULT_PATH).toString());
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
