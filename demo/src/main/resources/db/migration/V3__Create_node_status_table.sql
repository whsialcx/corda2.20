-- 创建节点状态历史表
CREATE TABLE IF NOT EXISTS node_status_history (
    id BIGSERIAL PRIMARY KEY,
    node_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_running BOOLEAN NOT NULL,
    process_id VARCHAR(50),
    cpu_usage DECIMAL(5,2),
    memory_usage BIGINT,
    last_start_time TIMESTAMP,
    last_stop_time TIMESTAMP,
    log_errors_count INTEGER DEFAULT 0,
    directory_size BIGINT,
    node_version VARCHAR(50),
    corda_version VARCHAR(50),
    checks JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建节点配置表
CREATE TABLE IF NOT EXISTS node_configurations (
    id BIGSERIAL PRIMARY KEY,
    node_name VARCHAR(100) UNIQUE NOT NULL,
    p2p_port INTEGER,
    rpc_port INTEGER,
    admin_port INTEGER,
    db_name VARCHAR(100),
    db_user VARCHAR(100),
    auto_ports BOOLEAN DEFAULT false,
    auto_db BOOLEAN DEFAULT false,
    node_config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_node_status_history_node_name ON node_status_history(node_name);
CREATE INDEX IF NOT EXISTS idx_node_status_history_created_at ON node_status_history(created_at);
CREATE INDEX IF NOT EXISTS idx_node_status_history_status ON node_status_history(status);
CREATE INDEX IF NOT EXISTS idx_node_status_history_running ON node_status_history(is_running);

CREATE INDEX IF NOT EXISTS idx_node_configurations_name ON node_configurations(node_name);
CREATE INDEX IF NOT EXISTS idx_node_configurations_ports ON node_configurations(p2p_port, rpc_port, admin_port);

-- 创建复合索引
CREATE INDEX IF NOT EXISTS idx_node_status_history_node_time ON node_status_history(node_name, created_at);
CREATE INDEX IF NOT EXISTS idx_node_status_history_running_time ON node_status_history(is_running, created_at);

COMMENT ON TABLE node_status_history IS '节点状态历史记录表';
COMMENT ON COLUMN node_status_history.node_name IS '节点名称';
COMMENT ON COLUMN node_status_history.status IS '节点状态：RUNNING, STOPPED, STARTING, STOPPING, ERROR';
COMMENT ON COLUMN node_status_history.is_running IS '是否正在运行';
COMMENT ON COLUMN node_status_history.process_id IS '进程ID';
COMMENT ON COLUMN node_status_history.cpu_usage IS 'CPU使用率';
COMMENT ON COLUMN node_status_history.memory_usage IS '内存使用量(字节)';
COMMENT ON COLUMN node_status_history.last_start_time IS '最后启动时间';
COMMENT ON COLUMN node_status_history.last_stop_time IS '最后停止时间';
COMMENT ON COLUMN node_status_history.log_errors_count IS '日志错误数量';
COMMENT ON COLUMN node_status_history.directory_size IS '节点目录大小';
COMMENT ON COLUMN node_status_history.checks IS '健康检查结果JSON';

COMMENT ON TABLE node_configurations IS '节点配置表';
COMMENT ON COLUMN node_configurations.node_name IS '节点名称';
COMMENT ON COLUMN node_configurations.p2p_port IS 'P2P端口';
COMMENT ON COLUMN node_configurations.rpc_port IS 'RPC端口';
COMMENT ON COLUMN node_configurations.admin_port IS '管理端口';
COMMENT ON COLUMN node_configurations.db_name IS '数据库名称';
COMMENT ON COLUMN node_configurations.db_user IS '数据库用户';
COMMENT ON COLUMN node_configurations.auto_ports IS '是否自动分配端口';
COMMENT ON COLUMN node_configurations.auto_db IS '是否自动生成数据库配置';
COMMENT ON COLUMN node_configurations.node_config IS '节点完整配置JSON';