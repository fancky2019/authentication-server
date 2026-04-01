-- drop  table sys_user;
-- drop  table  sys_role_menu;
-- 
-- drop  table  sys_role;
-- 
-- drop  table  sys_menu;
-- 
-- drop  table sys_dept;
-- 
-- drop  table sys_user_role;
-- 
-- drop  table  sys_user_dept;

-- ==================== 1. 用户表 ====================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(500) COMMENT '头像',
    gender TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    enabled TINYINT DEFAULT 1 COMMENT '启用状态: 1-启用 0-禁用',
    account_non_expired TINYINT DEFAULT 1 COMMENT '账号未过期: 1-未过期 0-过期',
    account_non_locked TINYINT DEFAULT 1 COMMENT '账号未锁定: 1-未锁定 0-锁定',
    credentials_non_expired TINYINT DEFAULT 1 COMMENT '密码未过期: 1-未过期 0-过期',
    last_login_time DATETIME COMMENT '最后登录时间',
    password_last_change_time datetime DEFAULT NULL COMMENT '密码最后修改时间',
    password_expire_days int DEFAULT '30' COMMENT '密码有效期(天)',
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==================== 2. 角色表 ====================
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码(如：ADMIN, MANAGER, USER)',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_sort INT DEFAULT 0 COMMENT '显示顺序',
    data_scope TINYINT DEFAULT 1 COMMENT '数据权限范围(1-全部 2-本级 3-本级及子级 4-自定义)',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ==================== 3. 权限表 ====================
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_type TINYINT NOT NULL COMMENT '权限类型(1-目录 2-菜单 3-按钮 4-接口 5-数据)',
    permission_code VARCHAR(100) COMMENT '权限标识(如：sys:user:view)',
    permission_value VARCHAR(200) COMMENT '权限值(如：/api/user/*)',
    path VARCHAR(200) COMMENT '路由路径(菜单类型使用)',
    component VARCHAR(200) COMMENT '组件路径(菜单类型使用)',
    icon VARCHAR(100) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    visible TINYINT DEFAULT 1 COMMENT '可见状态(1-显示 0-隐藏)',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    keep_alive TINYINT DEFAULT 1 COMMENT '缓存状态(1-缓存 0-不缓存)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ==================== 4. 用户角色关联表 ====================
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ==================== 5. 角色权限关联表 ====================
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    permission_type TINYINT COMMENT '权限类型(冗余字段，便于查询)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ==================== 6. 部门表 ====================
CREATE TABLE sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) COMMENT '部门编码',
    leader VARCHAR(50) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ==================== 7. 用户部门关联表 ====================
CREATE TABLE sys_user_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    is_main TINYINT DEFAULT 0 COMMENT '是否主部门(1-是 0-否)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    
    UNIQUE KEY uk_user_dept (user_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';

-- ==================== 8. 操作日志表 ====================
CREATE TABLE sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_result LONGTEXT COMMENT '响应结果',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    execute_time BIGINT COMMENT '执行耗时(毫秒)',
    status TINYINT DEFAULT 1 COMMENT '操作状态(1-成功 0-失败)',
    error_message TEXT COMMENT '错误信息',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ==================== 9. 数据字典表：存储字典分类信息 ====================
CREATE TABLE sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典ID',
    dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type TINYINT DEFAULT 1 COMMENT '字典类型(1-系统内置 2-自定义)',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- ==================== 10. 字典数据表：存储具体的字典项 ====================
CREATE TABLE sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    dict_code VARCHAR(50) NOT NULL COMMENT '字典编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_sort INT DEFAULT 0 COMMENT '字典排序',
    css_class VARCHAR(100) COMMENT '样式类',
    list_class VARCHAR(100) COMMENT '列表类',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认(1-是 0-否)',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ==================== 11. 客户端表：OAuth2.0认证授权机制。 ====================
CREATE TABLE sys_client (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户端ID',
    client_id VARCHAR(50) NOT NULL UNIQUE COMMENT '客户端标识',
    client_name VARCHAR(100) NOT NULL COMMENT '客户端名称',
    client_secret VARCHAR(255) COMMENT '客户端密钥',
    client_type TINYINT DEFAULT 1 COMMENT '客户端类型(1-WEB 2-APP 3-小程序)',
    grant_types VARCHAR(200) COMMENT '授权类型',
    redirect_uri VARCHAR(500) COMMENT '重定向URI',
    scope VARCHAR(200) COMMENT '权限范围',
    access_token_validity INT DEFAULT 7200 COMMENT '访问令牌有效期(秒)',
    refresh_token_validity INT DEFAULT 2592000 COMMENT '刷新令牌有效期(秒)',
    status TINYINT DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端表';

-- ==================== 12. 令牌表：存储和管理用户认证后的访问令牌，实现会话管理和令牌生命周期控制 
-- 使用jwt暂时不用====================
CREATE TABLE sys_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '令牌ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    client_id VARCHAR(50) NOT NULL COMMENT '客户端ID',
    access_token VARCHAR(500) NOT NULL COMMENT '访问令牌',
    refresh_token VARCHAR(500) COMMENT '刷新令牌',
    token_type VARCHAR(20) DEFAULT 'Bearer' COMMENT '令牌类型',
    expires_in INT COMMENT '过期时间(秒)',
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    expires_at DATETIME COMMENT '过期时间',
    revoked TINYINT DEFAULT 0 COMMENT '是否撤销(1-是 0-否)',
    
    -- 审计字段
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    delete_by VARCHAR(50) COMMENT '删除者',
    delete_time DATETIME COMMENT '删除时间',
    create_by VARCHAR(50) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(50) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 1 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='令牌表';



-- ==================== 初始化数据 ====================
-- 初始化超级管理员角色
INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark, version) 
VALUES ('ROLE_SUPER_ADMIN', '超级管理员', 1, 1, 1, '超级管理员，拥有所有权限', 1);

-- 初始化管理员角色
INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark, version) 
VALUES ('ROLE_ADMIN', '系统管理员', 2, 2, 1, '系统管理员，拥有大部分权限', 1);

-- 初始化普通用户角色
INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, remark, version) 
VALUES ('ROLE_USER', '普通用户', 3, 3, 1, '普通用户，只有基础权限', 1);

-- 初始化超级管理员用户 (密码: 123456，实际应用中应使用BCrypt加密)
INSERT INTO sys_user (username, password, nickname, email, phone, gender, enabled, account_non_expired, account_non_locked, credentials_non_expired, remark, version) 
VALUES ('admin', '$2a$10$rqX7QyQyQyQyQyQyQyQyQ', '超级管理员', 'admin@example.com', '13800000000', 1, 1, 1, 1, 1, '系统超级管理员', 1);

-- 初始化管理员用户 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, email, phone, gender, enabled, account_non_expired, account_non_locked, credentials_non_expired, remark, version) 
VALUES ('manager', '$2a$10$rqX7QyQyQyQyQyQyQyQyQ', '系统管理员', 'manager@example.com', '13800000001', 1, 1, 1, 1, 1, '系统管理员', 1);

-- 初始化普通用户 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, email, phone, gender, enabled, account_non_expired, account_non_locked, credentials_non_expired, remark, version) 
VALUES ('user', '$2a$10$rqX7QyQyQyQyQyQyQyQyQ', '普通用户', 'user@example.com', '13800000002', 2, 1, 1, 1, 1, '普通测试用户', 1);

-- 分配用户角色
INSERT INTO sys_user_role (user_id, role_id, remark, version) 
VALUES (1, 1, '超级管理员角色分配', 1), 
       (2, 2, '系统管理员角色分配', 1), 
       (3, 3, '普通用户角色分配', 1);

-- 初始化权限数据
-- 系统管理目录
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, icon, sort, visible, status, remark, version) 
VALUES (0, '系统管理', 1, 'system', 'system', 1, 1, 1, '系统管理目录', 1);

-- 用户管理菜单
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, path, component, icon, sort, visible, status, remark, version) 
VALUES (1, '用户管理', 2, 'system:user', '/user', 'system/user/index', 'user', 1, 1, 1, '用户管理菜单', 1);

-- 用户管理按钮权限
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (2, '用户查询', 3, 'system:user:query', 1, 1, 1, '查询用户按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (2, '用户新增', 3, 'system:user:add', 2, 1, 1, '新增用户按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (2, '用户编辑', 3, 'system:user:edit', 3, 1, 1, '编辑用户按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (2, '用户删除', 3, 'system:user:delete', 4, 1, 1, '删除用户按钮', 1);

-- 角色管理菜单
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, path, component, icon, sort, visible, status, remark, version) 
VALUES (1, '角色管理', 2, 'system:role', '/role', 'system/role/index', 'role', 2, 1, 1, '角色管理菜单', 1);

-- 角色管理按钮权限
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (7, '角色查询', 3, 'system:role:query', 1, 1, 1, '查询角色按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (7, '角色新增', 3, 'system:role:add', 2, 1, 1, '新增角色按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (7, '角色编辑', 3, 'system:role:edit', 3, 1, 1, '编辑角色按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (7, '角色删除', 3, 'system:role:delete', 4, 1, 1, '删除角色按钮', 1);

-- 权限管理菜单
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, path, component, icon, sort, visible, status, remark, version) 
VALUES (1, '权限管理', 2, 'system:permission', '/permission', 'system/permission/index', 'permission', 3, 1, 1, '权限管理菜单', 1);

-- 权限管理按钮权限
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (12, '权限查询', 3, 'system:permission:query', 1, 1, 1, '查询权限按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (12, '权限新增', 3, 'system:permission:add', 2, 1, 1, '新增权限按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (12, '权限编辑', 3, 'system:permission:edit', 3, 1, 1, '编辑权限按钮', 1);
INSERT INTO sys_permission (parent_id, permission_name, permission_type, permission_code, sort, visible, status, remark, version) 
VALUES (12, '权限删除', 3, 'system:permission:delete', 4, 1, 1, '删除权限按钮', 1);

-- 为超级管理员分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id, permission_type, remark, version)
SELECT 1, id, permission_type, '超级管理员权限分配', 1 FROM sys_permission WHERE deleted = 0;

-- 为管理员分配大部分权限
INSERT INTO sys_role_permission (role_id, permission_id, permission_type, remark, version)
SELECT 2, id, permission_type, '系统管理员权限分配', 1 FROM sys_permission 
WHERE deleted = 0 AND permission_code NOT LIKE '%delete%';

-- 为普通用户分配查询权限
INSERT INTO sys_role_permission (role_id, permission_id, permission_type, remark, version)
SELECT 3, id, permission_type, '普通用户权限分配', 1 FROM sys_permission 
WHERE deleted = 0 AND permission_code LIKE '%query%';

-- 初始化部门数据
INSERT INTO sys_dept (parent_id, dept_name, dept_code, leader, phone, email, status, sort, remark, version) 
VALUES (0, '总公司', 'DEPT_001', '张三', '010-12345678', 'zhangsan@example.com', 1, 1, '总公司部门', 1);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, leader, phone, email, status, sort, remark, version) 
VALUES (1, '技术部', 'DEPT_002', '李四', '010-12345679', 'lisi@example.com', 1, 1, '技术部门', 1);

INSERT INTO sys_dept (parent_id, dept_name, dept_code, leader, phone, email, status, sort, remark, version) 
VALUES (2, '开发组', 'DEPT_003', '王五', '010-12345680', 'wangwu@example.com', 1, 1, '开发小组', 1);

-- 初始化数据字典
INSERT INTO sys_dict (dict_code, dict_name, dict_type, status, remark, version) 
VALUES ('sys_user_status', '用户状态', 1, 1, '用户启用/禁用状态', 1);

INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, is_default, status, remark, version) 
VALUES ('sys_user_status', '启用', '1', 1, 1, 1, '用户启用状态', 1);
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, status, remark, version) 
VALUES ('sys_user_status', '禁用', '0', 2, 1, '用户禁用状态', 1);

INSERT INTO sys_dict (dict_code, dict_name, dict_type, status, remark, version) 
VALUES ('sys_user_gender', '用户性别', 1, 1, '用户性别', 1);

INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, is_default, status, remark, version) 
VALUES ('sys_user_gender', '未知', '0', 1, 1, 1, '未知性别', 1);
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, status, remark, version) 
VALUES ('sys_user_gender', '男', '1', 2, 1, '男性', 1);
INSERT INTO sys_dict_data (dict_code, dict_label, dict_value, dict_sort, status, remark, version) 
VALUES ('sys_user_gender', '女', '2', 3, 1, '女性', 1);

-- 初始化客户端
INSERT INTO sys_client (client_id, client_name, client_secret, client_type, grant_types, redirect_uri, scope, access_token_validity, refresh_token_validity, status, remark, version) 
VALUES ('web_client', 'Web客户端', '$2a$10$rqX7QyQyQyQyQyQyQyQyQ', 1, 'password,refresh_token', 'http://localhost:8080/callback', 'all', 7200, 2592000, 1, 'Web应用客户端', 1);

INSERT INTO sys_client (client_id, client_name, client_secret, client_type, grant_types, redirect_uri, scope, access_token_validity, refresh_token_validity, status, remark, version) 
VALUES ('app_client', 'APP客户端', '$2a$10$rqX7QyQyQyQyQyQyQyQyQ', 2, 'password,refresh_token', 'app://callback', 'basic', 7200, 2592000, 1, '移动应用客户端', 1);

select  *  from sys_user_role
select  *  from  sys_role
select  *  from  sys_permission
select  *  from  sys_role_permission
select  *  from sys_user

select
-- u.* ,
ur.role_id,
p.*
from  sys_user u
          join sys_user_role ur on u.id=ur.user_id
          join sys_role_permission rp on ur.role_id=rp.role_id
          join sys_permission p on rp.permission_id=p.id
where  1=1
  and  u.username='user'