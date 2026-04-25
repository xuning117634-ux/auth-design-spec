USE policy_center;

SET SESSION group_concat_max_len = 1000000;

DROP TABLE IF EXISTS pc_agent_permission_point_backup;

CREATE TABLE pc_agent_permission_point_backup AS
SELECT *
FROM pc_agent_permission_point;

DROP TABLE pc_agent_permission_point;

CREATE TABLE pc_agent_permission_point (
    agent_id VARCHAR(128) NOT NULL,
    enterprise VARCHAR(128) NOT NULL,
    permission_point_codes TEXT NOT NULL,
    last_sync_source VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id, enterprise)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO pc_agent_permission_point (
    agent_id,
    enterprise,
    permission_point_codes,
    last_sync_source,
    created_at,
    updated_at
)
SELECT
    agent_id,
    enterprise,
    GROUP_CONCAT(permission_point_code ORDER BY permission_point_code SEPARATOR ',') AS permission_point_codes,
    'migrate-agent-permission-snapshot' AS last_sync_source,
    MIN(created_at) AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM pc_agent_permission_point_backup
WHERE status = 'ACTIVE'
GROUP BY agent_id, enterprise;
