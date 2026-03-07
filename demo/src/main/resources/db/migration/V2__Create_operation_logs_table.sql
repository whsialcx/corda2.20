-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    target_node VARCHAR(100),
    parameters TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_logs_operation_type ON operation_logs(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_logs_target_node ON operation_logs(target_node);
CREATE INDEX IF NOT EXISTS idx_operation_logs_status ON operation_logs(status);
CREATE INDEX IF NOT EXISTS idx_operation_logs_created_at ON operation_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_operation_logs_start_time ON operation_logs(start_time);

-- 创建复合索引
CREATE INDEX IF NOT EXISTS idx_operation_logs_type_status ON operation_logs(operation_type, status);
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_operation ON operation_logs(user_id, operation_type);

COMMENT ON TABLE operation_logs IS '操作日志表';
COMMENT ON COLUMN operation_logs.user_id IS '操作用户ID';
COMMENT ON COLUMN operation_logs.username IS '操作用户名';
COMMENT ON COLUMN operation_logs.operation_type IS '操作类型：ADD_NODE, REMOVE_NODE, START_NODE, STOP_NODE, DEPLOY_NETWORK';
COMMENT ON COLUMN operation_logs.target_node IS '目标节点名称';
COMMENT ON COLUMN operation_logs.parameters IS '操作参数JSON';
COMMENT ON COLUMN operation_logs.status IS '操作状态：SUCCESS, FAILED, IN_PROGRESS';
COMMENT ON COLUMN operation_logs.error_message IS '错误信息';
COMMENT ON COLUMN operation_logs.start_time IS '操作开始时间';
COMMENT ON COLUMN operation_logs.end_time IS '操作结束时间';
COMMENT ON COLUMN operation_logs.duration_ms IS '操作耗时(毫秒)';
COMMENT ON COLUMN operation_logs.ip_address IS '操作IP地址';
COMMENT ON COLUMN operation_logs.user_agent IS '用户代理信息';