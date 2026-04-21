CREATE DATABASE IF NOT EXISTS policy_center
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE policy_center;

CREATE TABLE IF NOT EXISTS pc_tool (
    tool_id VARCHAR(255) NOT NULL PRIMARY KEY,
    display_name_zh VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pc_permission_point (
    permission_point_code VARCHAR(128) NOT NULL PRIMARY KEY,
    display_name_zh VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_sync_source VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pc_permission_point_tool_rel (
    permission_point_code VARCHAR(128) NOT NULL,
    tool_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_point_code, tool_id),
    KEY idx_pc_permission_point_tool_rel_tool_id (tool_id),
    KEY idx_pc_permission_point_tool_rel_permission_point_code (permission_point_code),
    CONSTRAINT fk_pc_rel_permission_point
        FOREIGN KEY (permission_point_code) REFERENCES pc_permission_point(permission_point_code),
    CONSTRAINT fk_pc_rel_tool
        FOREIGN KEY (tool_id) REFERENCES pc_tool(tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pc_agent_strategy (
    strategy_id VARCHAR(128) NOT NULL PRIMARY KEY,
    agent_id VARCHAR(128) NOT NULL,
    permission_point_code VARCHAR(128) NOT NULL,
    condition_field VARCHAR(64) NOT NULL,
    condition_operator VARCHAR(16) NOT NULL,
    effect VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pc_agent_strategy_agent_perm_status (agent_id, permission_point_code, status),
    CONSTRAINT fk_pc_strategy_permission_point
        FOREIGN KEY (permission_point_code) REFERENCES pc_permission_point(permission_point_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pc_agent_strategy_condition_value (
    strategy_id VARCHAR(128) NOT NULL,
    value_order INT NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (strategy_id, value_order),
    CONSTRAINT fk_pc_strategy_condition_strategy
        FOREIGN KEY (strategy_id) REFERENCES pc_agent_strategy(strategy_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
