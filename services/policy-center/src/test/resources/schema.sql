DROP TABLE IF EXISTS pc_agent_strategy_condition_value;
DROP TABLE IF EXISTS pc_agent_strategy;
DROP TABLE IF EXISTS pc_agent_permission_point;
DROP TABLE IF EXISTS pc_permission_point_tool_rel;
DROP TABLE IF EXISTS pc_permission_point;
DROP TABLE IF EXISTS pc_tool;

CREATE TABLE pc_tool (
    tool_id VARCHAR(255) PRIMARY KEY,
    display_name_zh VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pc_permission_point (
    permission_point_code VARCHAR(128) PRIMARY KEY,
    enterprise VARCHAR(128) NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    display_name_zh VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_sync_source VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pc_permission_point_tool_rel (
    permission_point_code VARCHAR(128) NOT NULL,
    tool_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_point_code, tool_id),
    CONSTRAINT fk_pc_rel_permission_point
        FOREIGN KEY (permission_point_code) REFERENCES pc_permission_point(permission_point_code),
    CONSTRAINT fk_pc_rel_tool
        FOREIGN KEY (tool_id) REFERENCES pc_tool(tool_id)
);

CREATE TABLE pc_agent_permission_point (
    agent_id VARCHAR(128) NOT NULL,
    enterprise VARCHAR(128) NOT NULL,
    permission_point_codes VARCHAR(4000) NOT NULL,
    last_sync_source VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id, enterprise)
);

CREATE TABLE pc_agent_strategy (
    strategy_id VARCHAR(128) PRIMARY KEY,
    agent_id VARCHAR(128) NOT NULL,
    permission_point_code VARCHAR(128) NOT NULL,
    condition_field VARCHAR(64) NOT NULL,
    condition_operator VARCHAR(16) NOT NULL,
    effect VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pc_strategy_permission_point
        FOREIGN KEY (permission_point_code) REFERENCES pc_permission_point(permission_point_code)
);

CREATE TABLE pc_agent_strategy_condition_value (
    strategy_id VARCHAR(128) NOT NULL,
    value_order INT NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (strategy_id, value_order),
    CONSTRAINT fk_pc_strategy_condition_strategy
        FOREIGN KEY (strategy_id) REFERENCES pc_agent_strategy(strategy_id) ON DELETE CASCADE
);

CREATE INDEX idx_pc_permission_point_tool_rel_tool_id
    ON pc_permission_point_tool_rel (tool_id);

CREATE INDEX idx_pc_permission_point_tool_rel_permission_point_code
    ON pc_permission_point_tool_rel (permission_point_code);

CREATE INDEX idx_pc_permission_point_enterprise_app_status
    ON pc_permission_point (enterprise, app_id, status);

CREATE INDEX idx_pc_agent_strategy_agent_perm_status
    ON pc_agent_strategy (agent_id, permission_point_code, status);
