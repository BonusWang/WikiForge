package com.wikiforge.common.filesystem;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathSafetyTests {

    @Test
    void normalizeAbsoluteRejectsRelativePath() {
        assertThatThrownBy(() -> PathSafety.normalizeAbsolute(Path.of("relative/source")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOURCE_INVALID_PATH);
    }

    @Test
    void ensureNoOverlapRejectsInputInsideRawSourcesRoot() {
        Path rawSourcesRoot = Path.of("E:/WikiForge_RawSources").toAbsolutePath().normalize();
        Path inputPath = rawSourcesRoot.resolve("already-managed").normalize();

        assertThatThrownBy(() -> PathSafety.ensureNoOverlap(inputPath, rawSourcesRoot))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must not overlap");
    }

    @Test
    void ensureNoOverlapRejectsRawSourcesInsideInput() {
        Path inputPath = Path.of("E:/knowledge").toAbsolutePath().normalize();
        Path rawSourcesRoot = inputPath.resolve("WikiForge_RawSources").normalize();

        assertThatThrownBy(() -> PathSafety.ensureNoOverlap(inputPath, rawSourcesRoot))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must not overlap");
    }

    @Test
    void ensureNoOverlapAllowsSiblingDirectories() {
        Path inputPath = Path.of("E:/knowledge/messy").toAbsolutePath().normalize();
        Path rawSourcesRoot = Path.of("E:/knowledge/WikiForge_RawSources").toAbsolutePath().normalize();

        PathSafety.ensureNoOverlap(inputPath, rawSourcesRoot);

        assertThat(inputPath).isNotEqualTo(rawSourcesRoot);
    }
}
