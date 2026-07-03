-- ============================================================
-- 可复用用户管理服务 - 数据库建表脚本
-- 基于 PDF 文档 E-R 图设计
-- ============================================================

-- 1. 用户主体表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    mobile VARCHAR(32) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED / PENDING_CANCEL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_mobile (mobile),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主体表';

-- 2. 登录凭证表 (支持一人多凭证)
CREATE TABLE IF NOT EXISTS credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credential_type VARCHAR(32) NOT NULL COMMENT 'USERNAME / EMAIL / MOBILE',
    credential_value VARCHAR(128) NOT NULL,
    password_hash VARCHAR(256) DEFAULT NULL COMMENT 'SHA-256 哈希',
    salt VARCHAR(64) DEFAULT NULL COMMENT '随机盐值',
    is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主凭证',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_credential (credential_type, credential_value),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录凭证表';

-- 3. 第三方绑定表
CREATE TABLE IF NOT EXISTS third_party_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL COMMENT 'feishu / wechat / github 等',
    open_id VARCHAR(128) NOT NULL,
    extra_info JSON DEFAULT NULL COMMENT '第三方返回的额外信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_provider_openid (provider, open_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_tpb_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方绑定表';

-- 4. 验证码记录表
CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target VARCHAR(128) NOT NULL COMMENT '邮箱或手机号',
    scene VARCHAR(32) NOT NULL DEFAULT 'login' COMMENT 'login / register / reset_password',
    code VARCHAR(16) NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target_scene (target, scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码记录表';

-- 5. 用户组表
CREATE TABLE IF NOT EXISTS user_group_defs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_group_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组表';

-- 6. 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(128) NOT NULL COMMENT '权限标识符，如 user:read, user:write',
    name VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) DEFAULT NULL COMMENT 'API / MENU / DATA',
    description VARCHAR(256) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 7. 用户-用户组关系表
CREATE TABLE IF NOT EXISTS user_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_group (user_id, group_id),
    CONSTRAINT fk_ug_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ug_group FOREIGN KEY (group_id) REFERENCES user_group_defs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-用户组关系表';

-- 8. 用户组-权限关系表
CREATE TABLE IF NOT EXISTS group_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_group_permission (group_id, permission_id),
    CONSTRAINT fk_gp_group FOREIGN KEY (group_id) REFERENCES user_group_defs(id) ON DELETE CASCADE,
    CONSTRAINT fk_gp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组-权限关系表';

-- 9. 用户-权限直接授权表
CREATE TABLE IF NOT EXISTS user_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_permission (user_id, permission_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_up_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-权限关系表';

-- 10. 自定义属性定义表
CREATE TABLE IF NOT EXISTS attribute_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attr_key VARCHAR(64) NOT NULL,
    attr_name VARCHAR(64) NOT NULL,
    attr_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT 'STRING / NUMBER / BOOLEAN / DATE',
    is_hot TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门字段（热字段建虚拟列加速查询）',
    description VARCHAR(256) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_attr_key (attr_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义属性定义表';

-- 11. 用户属性值表 (JSON 存储可扩展字段)
CREATE TABLE IF NOT EXISTS user_attribute_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    attr_key VARCHAR(64) NOT NULL,
    attr_value TEXT NOT NULL COMMENT 'JSON 格式存储',
    -- 热门字段虚拟列（MySQL 5.7+ 虚拟列索引加速查询）
    value_str VARCHAR(256) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(attr_value, '$.value'))) VIRTUAL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_attr (user_id, attr_key),
    INDEX idx_value_str (value_str),
    CONSTRAINT fk_uav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户属性值表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认用户组
INSERT IGNORE INTO user_group_defs (name, description) VALUES
    ('admin', '管理员组'),
    ('user', '普通用户组');

-- 默认权限
INSERT IGNORE INTO permissions (code, name, resource_type, description) VALUES
    ('user:read', '查看用户', 'API', '查看用户信息权限'),
    ('user:write', '修改用户', 'API', '修改用户信息权限'),
    ('user:delete', '删除用户', 'API', '删除用户权限'),
    ('user:admin', '管理用户', 'API', '用户管理权限'),
    ('auth:login', '登录', 'API', '登录权限'),
    ('auth:register', '注册', 'API', '注册权限');

-- 管理员组拥有所有权限
INSERT IGNORE INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM user_group_defs g, permissions p WHERE g.name = 'admin';

-- 普通用户组拥有基本权限
INSERT IGNORE INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM user_group_defs g, permissions p
WHERE g.name = 'user' AND p.code IN ('auth:login', 'auth:register', 'user:read');

-- 默认属性定义
INSERT IGNORE INTO attribute_definitions (attr_key, attr_name, attr_type, is_hot, description) VALUES
    ('studentNo', '学号', 'STRING', 1, '学生学号'),
    ('college', '学院', 'STRING', 1, '所属学院'),
    ('department', '部门', 'STRING', 0, '所属部门'),
    ('role', '角色', 'STRING', 1, '业务角色标签'),
    ('tag', '标签', 'STRING', 0, '用户标签');
