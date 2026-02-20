-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    role VARCHAR(20) DEFAULT 'USER'
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 插入初始管理员用户 (密码: admin123)
INSERT INTO users (username, password, email, full_name, role) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVwUi.', 'admin@corda-manager.com', '系统管理员', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

COMMENT ON TABLE users IS '系统用户表';
COMMENT ON COLUMN users.username IS '用户名，唯一';
COMMENT ON COLUMN users.password IS '加密密码';
COMMENT ON COLUMN users.email IS '邮箱地址';
COMMENT ON COLUMN users.full_name IS '用户全名';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.is_active IS '是否激活';
COMMENT ON COLUMN users.role IS '用户角色：ADMIN, USER';