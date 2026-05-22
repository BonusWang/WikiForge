CREATE TABLE system_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(128) NOT NULL,
    setting_value TEXT NULL,
    setting_type VARCHAR(32) NOT NULL DEFAULT 'string',
    description VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE model_providers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    api_base_url VARCHAR(512) NULL,
    default_model VARCHAR(128) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    config_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_providers_key (provider_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
