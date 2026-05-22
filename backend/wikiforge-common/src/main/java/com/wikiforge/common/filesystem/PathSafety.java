package com.wikiforge.common.filesystem;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import java.nio.file.Path;

public final class PathSafety {

    private PathSafety() {
    }

    public static Path normalizeAbsolute(Path path) {
        if (path == null || !path.isAbsolute()) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "path must be absolute");
        }
        return path.normalize();
    }

    public static void ensureNoOverlap(Path inputPath, Path rawSourcesRoot) {
        Path normalizedInputPath = normalizeAbsolute(inputPath);
        Path normalizedRawSourcesRoot = normalizeAbsolute(rawSourcesRoot);

        if (normalizedInputPath.startsWith(normalizedRawSourcesRoot)
                || normalizedRawSourcesRoot.startsWith(normalizedInputPath)) {
            throw new BusinessException(
                    ErrorCode.SOURCE_INVALID_PATH,
                    "inputPath must not overlap rawSourcesRoot"
            );
        }
    }
}
