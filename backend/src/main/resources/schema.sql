CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '1:active, 0:disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name VARCHAR(50) NOT NULL,
    perm_code VARCHAR(50) NOT NULL UNIQUE,
    type TINYINT COMMENT '1:menu, 2:button',
    parent_id BIGINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS act_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS act_activity_tag (
    activity_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO act_tag (name, color) VALUES ('学术讲座', '#409EFF'), ('社团活动', '#67C23A'), ('体育赛事', '#E6A23C'), ('文艺演出', '#F56C6C'), ('志愿服务', '#909399') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO sys_role (id, role_name, role_code, description) VALUES (1, '超级管理员', 'ROLE_SUPER_ADMIN', '系统最高权限管理员') ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description);
INSERT INTO sys_user (id, username, password, email, status) VALUES (1, 'admin', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'admin@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1) ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO sys_role (id, role_name, role_code, description) VALUES (3, '辅导员', 'ROLE_COUNSELOR', '活动审核人员') ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description);
INSERT INTO sys_role (id, role_name, role_code, description) VALUES (4, '社团负责人', 'ROLE_CLUB_OWNER', '活动发起人') ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description);
INSERT INTO sys_role (id, role_name, role_code, description) VALUES (5, '学生', 'ROLE_STUDENT', '普通学生用户') ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description);

INSERT INTO sys_user (id, username, password, email, status) VALUES (2, 'admin2', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'admin2@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);
INSERT INTO sys_user (id, username, password, email, status) VALUES (3, 'counselor1', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'counselor1@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);
INSERT INTO sys_user (id, username, password, email, status) VALUES (4, 'owner1', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'owner1@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);
INSERT INTO sys_user (id, username, password, email, status) VALUES (5, 'student1', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'student1@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);
INSERT INTO sys_user (id, username, password, email, status) VALUES (6, 'student2', '$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS', 'student2@example.com', 1) ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), status=VALUES(status);

INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 1) ON DUPLICATE KEY UPDATE user_id=user_id;
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 3) ON DUPLICATE KEY UPDATE user_id=user_id;
INSERT INTO sys_user_role (user_id, role_id) VALUES (4, 4) ON DUPLICATE KEY UPDATE user_id=user_id;
INSERT INTO sys_user_role (user_id, role_id) VALUES (5, 5) ON DUPLICATE KEY UPDATE user_id=user_id;
INSERT INTO sys_user_role (user_id, role_id) VALUES (6, 5) ON DUPLICATE KEY UPDATE user_id=user_id;

CREATE TABLE IF NOT EXISTS act_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    cover_url VARCHAR(1024),
    content LONGTEXT,
    content_type VARCHAR(50),
    location VARCHAR(255),
    start_time DATETIME,
    end_time DATETIME,
    reg_start_time DATETIME,
    reg_end_time DATETIME,
    form_schema LONGTEXT,
    channels LONGTEXT,
    whitelist_enabled TINYINT DEFAULT 0,
    stock_total INT DEFAULT 0,
    stock_available INT DEFAULT 0,
    per_user_limit INT DEFAULT 0,
    current_variant VARCHAR(10) DEFAULT 'A',
    status VARCHAR(20) NOT NULL,
    audit_reason VARCHAR(255),
    audit_by BIGINT,
    audit_at DATETIME,
    publish_at DATETIME,
    offline_at DATETIME,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_act_status_publish (status, publish_at),
    KEY idx_act_created_by (created_by),
    KEY idx_act_title (title)
);

CREATE TABLE IF NOT EXISTS act_activity_variant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    variant_code VARCHAR(10) NOT NULL,
    variant_version INT NOT NULL,
    title VARCHAR(255),
    summary VARCHAR(500),
    cover_url VARCHAR(1024),
    content LONGTEXT,
    content_type VARCHAR(50),
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_variant (activity_id, variant_code, variant_version),
    KEY idx_activity_variant_latest (activity_id, variant_code, variant_version)
);

CREATE TABLE IF NOT EXISTS act_activity_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    operator_id BIGINT,
    op_type VARCHAR(50) NOT NULL,
    before_data LONGTEXT,
    after_data LONGTEXT,
    diff_data LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_activity_log_activity (activity_id, id)
);

CREATE TABLE IF NOT EXISTS act_activity_whitelist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_user (activity_id, user_id),
    KEY idx_activity_whitelist_activity (activity_id)
);

CREATE TABLE IF NOT EXISTS act_registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    extra_data LONGTEXT,
    audit_reason VARCHAR(500),
    audit_by BIGINT,
    audit_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_registration (activity_id, user_id),
    KEY idx_registration_activity (activity_id, id),
    KEY idx_registration_user (user_id, id),
    KEY idx_registration_activity_status (activity_id, status, id)
);

CREATE TABLE IF NOT EXISTS act_track_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL,
    user_id BIGINT,
    activity_id BIGINT,
    event_data LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_track_event_name_time (event_name, id),
    KEY idx_track_event_activity (activity_id, id)
);
