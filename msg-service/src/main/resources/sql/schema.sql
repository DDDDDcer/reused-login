CREATE TABLE IF NOT EXISTS message_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_content TEXT NOT NULL,
    carrier_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_variables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    variable_name VARCHAR(64) NOT NULL,
    description VARCHAR(256) DEFAULT NULL,
    required TINYINT NOT NULL DEFAULT 1,
    default_value VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_template_variable (template_id, variable_name),
    CONSTRAINT fk_variable_template FOREIGN KEY (template_id) REFERENCES message_templates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT DEFAULT NULL,
    title VARCHAR(256) DEFAULT NULL,
    content TEXT NOT NULL,
    variables_json JSON DEFAULT NULL,
    sender_id VARCHAR(64) NOT NULL,
    receiver_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_receiver (receiver_id),
    INDEX idx_message_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_carriers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_type VARCHAR(32) NOT NULL,
    carrier_name VARCHAR(128) NOT NULL,
    description VARCHAR(256) DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_carrier_type (carrier_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS carrier_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id BIGINT NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    access_key VARCHAR(256) DEFAULT NULL,
    config_json JSON DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_account_name (carrier_id, account_name),
    INDEX idx_account_carrier_status (carrier_id, status),
    CONSTRAINT fk_account_carrier FOREIGN KEY (carrier_id) REFERENCES message_carriers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_strategies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_name VARCHAR(128) NOT NULL,
    max_retries INT NOT NULL DEFAULT 0,
    retry_interval_seconds INT NOT NULL DEFAULT 60,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_strategy_name (strategy_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    carrier_id BIGINT NOT NULL,
    account_id BIGINT DEFAULT NULL,
    strategy_id BIGINT DEFAULT NULL,
    carrier_type VARCHAR(32) NOT NULL,
    receiver_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    biz_code VARCHAR(64) NOT NULL DEFAULT 'topbiz',
    scene_code VARCHAR(64) NOT NULL DEFAULT 'default',
    idempotency_key VARCHAR(128) DEFAULT NULL,
    request_hash VARCHAR(128) DEFAULT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    local_deleted TINYINT NOT NULL DEFAULT 0,
    planned_time DATETIME DEFAULT NULL,
    next_retry_time DATETIME DEFAULT NULL,
    sent_at DATETIME DEFAULT NULL,
    fail_reason VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_task_idempotency (biz_code, scene_code, idempotency_key),
    INDEX idx_task_schedule (status, planned_time),
    INDEX idx_task_retry (status, next_retry_time),
    INDEX idx_task_filter (carrier_type, receiver_id, status, created_at),
    CONSTRAINT fk_task_message FOREIGN KEY (message_id) REFERENCES messages(id),
    CONSTRAINT fk_task_carrier FOREIGN KEY (carrier_id) REFERENCES message_carriers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_response TEXT DEFAULT NULL,
    fail_reason VARCHAR(512) DEFAULT NULL,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_task (task_id, attempt_no),
    CONSTRAINT fk_log_task FOREIGN KEY (task_id) REFERENCES message_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO message_carriers (carrier_type, carrier_name, description, status) VALUES
    ('LOCAL', 'Local message', 'In-app local message', 'ENABLED'),
    ('SMS', 'SMS', 'Mock SMS carrier', 'ENABLED'),
    ('EMAIL', 'Email', 'Mock email carrier', 'ENABLED'),
    ('FEISHU', 'Feishu', 'Mock Feishu carrier', 'ENABLED');

INSERT IGNORE INTO carrier_accounts (carrier_id, account_name, provider, config_json, status)
SELECT id, CONCAT(carrier_type, '-default'), 'MOCK', JSON_OBJECT('mock', true), 'ENABLED'
FROM message_carriers;

INSERT IGNORE INTO message_strategies (strategy_name, max_retries, retry_interval_seconds, status)
VALUES ('default', 2, 60, 'ENABLED');
