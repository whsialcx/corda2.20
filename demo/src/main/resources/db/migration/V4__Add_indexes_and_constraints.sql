-- 添加外键约束
ALTER TABLE operation_logs 
ADD CONSTRAINT fk_operation_logs_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 添加检查约束
ALTER TABLE users 
ADD CONSTRAINT chk_users_role 
CHECK (role IN ('ADMIN', 'USER', 'OPERATOR'));

ALTER TABLE operation_logs 
ADD CONSTRAINT chk_operation_logs_status 
CHECK (status IN ('SUCCESS', 'FAILED', 'IN_PROGRESS', 'CANCELLED'));

ALTER TABLE operation_logs 
ADD CONSTRAINT chk_operation_logs_operation_type 
CHECK (operation_type IN (
    'ADD_NODE', 'REMOVE_NODE', 'START_NODE', 'STOP_NODE', 
    'DEPLOY_NETWORK', 'VALIDATE_PROJECT', 'LIST_NODES'
));

ALTER TABLE node_status_history 
ADD CONSTRAINT chk_node_status_history_status 
CHECK (status IN ('RUNNING', 'STOPPED', 'STARTING', 'STOPPING', 'ERROR', 'UNKNOWN'));

-- 添加唯一约束
ALTER TABLE node_configurations 
ADD CONSTRAINT uq_node_configurations_ports 
UNIQUE (p2p_port, rpc_port, admin_port);

-- 创建函数：更新updated_at时间戳
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 创建触发器：自动更新updated_at
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_node_configurations_updated_at 
    BEFORE UPDATE ON node_configurations 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 创建视图：节点状态汇总
CREATE OR REPLACE VIEW node_status_summary AS
SELECT 
    nsh.node_name,
    nsh.status as current_status,
    nsh.is_running,
    nsh.last_start_time,
    nsh.last_stop_time,
    nsh.created_at as last_check_time,
    nc.p2p_port,
    nc.rpc_port,
    nc.admin_port,
    (SELECT COUNT(*) FROM node_status_history 
     WHERE node_name = nsh.node_name AND created_at >= CURRENT_DATE) as today_checks,
    (SELECT COUNT(*) FROM operation_logs 
     WHERE target_node = nsh.node_name AND status = 'FAILED' 
     AND created_at >= (CURRENT_DATE - INTERVAL '7 days')) as recent_failures
FROM node_status_history nsh
LEFT JOIN node_configurations nc ON nsh.node_name = nc.node_name
WHERE nsh.created_at = (
    SELECT MAX(created_at) 
    FROM node_status_history 
    WHERE node_name = nsh.node_name
);

-- 创建视图：操作统计
CREATE OR REPLACE VIEW operation_statistics AS
SELECT 
    operation_type,
    status,
    COUNT(*) as operation_count,
    AVG(duration_ms) as avg_duration,
    MIN(duration_ms) as min_duration,
    MAX(duration_ms) as max_duration,
    DATE(start_time) as operation_date
FROM operation_logs
WHERE start_time >= (CURRENT_DATE - INTERVAL '30 days')
GROUP BY operation_type, status, DATE(start_time)
ORDER BY operation_date DESC, operation_count DESC;

COMMENT ON VIEW node_status_summary IS '节点状态汇总视图';
COMMENT ON VIEW operation_statistics IS '操作统计视图';