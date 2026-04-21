INSERT INTO pc_tool (tool_id, display_name_zh, created_at, updated_at) VALUES
('mcp:financial-report-server/query_monthly_report', '查询月度报表', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('mcp:contract-server/get_contract', '查询合同详情', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('mcp:invoice-server/query_invoices', '查询发票', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO pc_permission_point (
    permission_point_code,
    display_name_zh,
    description,
    status,
    last_sync_source,
    created_at,
    updated_at
) VALUES
('erp:report:r', 'ERP 报表的可读权限', '允许读取 ERP 报表数据', 'ACTIVE', 'seed-demo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('erp:contract:r', 'ERP 合同的可读权限', '允许读取 ERP 合同数据', 'ACTIVE', 'seed-demo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('erp:invoice:r', 'ERP 发票的可读权限', '允许读取 ERP 发票数据', 'ACTIVE', 'seed-demo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO pc_permission_point_tool_rel (permission_point_code, tool_id, created_at) VALUES
('erp:report:r', 'mcp:financial-report-server/query_monthly_report', CURRENT_TIMESTAMP),
('erp:contract:r', 'mcp:contract-server/get_contract', CURRENT_TIMESTAMP),
('erp:invoice:r', 'mcp:invoice-server/query_invoices', CURRENT_TIMESTAMP);

INSERT INTO pc_agent_strategy (
    strategy_id,
    agent_id,
    permission_point_code,
    condition_field,
    condition_operator,
    effect,
    status,
    created_at,
    updated_at
) VALUES
('stg_contract_permit_demo_user', 'agt_business_001', 'erp:contract:r', 'subject.user_id', 'in', 'PERMIT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('stg_invoice_deny_blocked_user', 'agt_business_001', 'erp:invoice:r', 'subject.user_id', 'in', 'DENY', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO pc_agent_strategy_condition_value (strategy_id, value_order, condition_value) VALUES
('stg_contract_permit_demo_user', 0, 'z01062668'),
('stg_invoice_deny_blocked_user', 0, 'blocked_demo_user');
