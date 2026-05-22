package com.wikiforge.common.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTests {

    @Test
    void failIncludesCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.fail("SOURCE_001", "invalid path");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("invalid path");
        assertThat(response.code()).isEqualTo("SOURCE_001");
    }

    @Test
    void okKeepsCodeEmpty() {
        ApiResponse<String> response = ApiResponse.ok("ready");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ready");
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.code()).isNull();
    }
}
