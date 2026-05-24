package com.wikiforge.core.application.service;

import com.wikiforge.core.application.dto.VersionInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VersionService {

    private final VersionInfoResponse versionInfo;

    public VersionService(
            @Value("${wikiforge.release.version}") String version,
            @Value("${wikiforge.release.stage}") String stage,
            @Value("${wikiforge.release.release-date}") String releaseDate
    ) {
        this.versionInfo = new VersionInfoResponse(
                "WikiForge",
                "wikiforge-core-service",
                version,
                stage,
                releaseDate,
                "/api/v1"
        );
    }

    public VersionInfoResponse getVersion() {
        return versionInfo;
    }
}
