-- ============================================
-- Initialize System Data for inkwash
-- ============================================

-- Clean existing data (in reverse order for foreign keys)
DELETE FROM sys_role_permission;
DELETE FROM sys_user_group;
DELETE FROM sys_group_role;
DELETE FROM sys_identity;
DELETE FROM sys_account;
DELETE FROM sys_user;
DELETE FROM sys_group;
DELETE FROM sys_role;
DELETE FROM sys_permission;
DELETE FROM sys_menu;
DELETE FROM cms_article_term;
DELETE FROM cms_term;
DELETE FROM cms_comment;
DELETE FROM cms_category;

-- ============================================
-- 1. Initialize Roles (sys_role)
-- ============================================
INSERT INTO sys_role (id, name, code, status, remark, create_time, update_time) VALUES
(1, 'system', 'ROLE_SYSTEM', 1, '拥有系统所有权限', NOW(), NOW()),
(2, 'admin', 'ROLE_ADMIN', 1, '系统管理权限', NOW(), NOW()),
(3, 'editor', 'ROLE_EDITOR', 1, '内容编辑权限', NOW(), NOW()),
(4, 'user', 'ROLE_USER', 1, '普通用户权限', NOW(), NOW());

-- ============================================
-- 2. Initialize Permissions (sys_permission)
-- ============================================
-- System Module Permissions
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(1, '查看用户', 3, 'system', 'user', 'query', 1, '查看用户', NOW(), NOW()),
(2, '创建用户', 3, 'system', 'user', 'create', 1, '创建用户', NOW(), NOW()),
(3, '编辑用户', 3, 'system', 'user', 'update', 1, '编辑用户', NOW(), NOW()),
(4, '删除用户', 3, 'system', 'user', 'delete', 1, '删除用户', NOW(), NOW()),
(5, '用户详情', 3, 'system', 'user', 'detail', 1, '查看用户详情', NOW(), NOW()),
(26, '分配用户组', 3, 'system', 'user', 'assign-group', 1, '分配用户组', NOW(), NOW()),
(27, '重置密码', 3, 'system', 'user', 'reset-password', 1, '重置用户密码', NOW(), NOW()),
(28, '更新状态', 3, 'system', 'user', 'update-status', 1, '更新用户状态', NOW(), NOW()),

(6, '查看角色', 3, 'system', 'role', 'query', 1, '查看角色', NOW(), NOW()),
(7, '创建角色', 3, 'system', 'role', 'create', 1, '创建角色', NOW(), NOW()),
(8, '编辑角色', 3, 'system', 'role', 'update', 1, '编辑角色', NOW(), NOW()),
(9, '删除角色', 3, 'system', 'role', 'delete', 1, '删除角色', NOW(), NOW()),
(10, '角色详情', 3, 'system', 'role', 'detail', 1, '查看角色详情', NOW(), NOW()),
(30, '分配权限', 3, 'system', 'role', 'assign-permission', 1, '为角色分配权限', NOW(), NOW()),

(11, '查看分组', 3, 'system', 'group', 'query', 1, '查看分组', NOW(), NOW()),
(12, '创建分组', 3, 'system', 'group', 'create', 1, '创建分组', NOW(), NOW()),
(13, '编辑分组', 3, 'system', 'group', 'update', 1, '编辑分组', NOW(), NOW()),
(14, '删除分组', 3, 'system', 'group', 'delete', 1, '删除分组', NOW(), NOW()),
(15, '分组详情', 3, 'system', 'group', 'detail', 1, '查看分组详情', NOW(), NOW()),
(29, '分配角色', 3, 'system', 'group', 'assign-role', 1, '分配角色给分组', NOW(), NOW()),

(16, '查看菜单', 3, 'system', 'menu', 'query', 1, '查看菜单', NOW(), NOW()),
(17, '创建菜单', 3, 'system', 'menu', 'create', 1, '创建菜单', NOW(), NOW()),
(18, '编辑菜单', 3, 'system', 'menu', 'update', 1, '编辑菜单', NOW(), NOW()),
(19, '删除菜单', 3, 'system', 'menu', 'delete', 1, '删除菜单', NOW(), NOW()),
(20, '菜单详情', 3, 'system', 'menu', 'detail', 1, '查看菜单详情', NOW(), NOW()),
(21, '查看权限', 3, 'system', 'permission', 'query', 1, '查看权限', NOW(), NOW()),
(22, '创建权限', 3, 'system', 'permission', 'create', 1, '创建权限', NOW(), NOW()),
(23, '编辑权限', 3, 'system', 'permission', 'update', 1, '编辑权限', NOW(), NOW()),
(24, '删除权限', 3, 'system', 'permission', 'delete', 1, '删除权限', NOW(), NOW()),
(25, '权限详情', 3, 'system', 'permission', 'detail', 1, '查看权限详情', NOW(), NOW()),
(31, '查看通知', 3, 'system', 'notice', 'query', 1, '查看通知', NOW(), NOW()),
(32, '创建通知', 3, 'system', 'notice', 'create', 1, '创建通知', NOW(), NOW()),
(33, '编辑通知', 3, 'system', 'notice', 'update', 1, '编辑通知', NOW(), NOW()),
(34, '删除通知', 3, 'system', 'notice', 'delete', 1, '删除通知', NOW(), NOW()),
(35, '通知详情', 3, 'system', 'notice', 'detail', 1, '查看通知详情', NOW(), NOW()),
(36, '更新账号', 3, 'system', 'account', 'update', 1, '更新账号信息', NOW(), NOW()),
(37, '查询账号', 3, 'system', 'account', 'query', 1, '查询账号信息', NOW(), NOW()),
(38, '删除文件', 3, 'system', 'file', 'delete', 1, '删除文件', NOW(), NOW());

-- CMS Module Permissions (Articles)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(101, '查看文章', 3, 'cms', 'article', 'query', 1, '查看文章', NOW(), NOW()),
(102, '创建文章', 3, 'cms', 'article', 'create', 1, '创建文章', NOW(), NOW()),
(103, '编辑文章', 3, 'cms', 'article', 'update', 1, '编辑文章', NOW(), NOW()),
(104, '删除文章', 3, 'cms', 'article', 'delete', 1, '删除文章', NOW(), NOW()),
(105, '文章详情', 3, 'cms', 'article', 'detail', 1, '查看文章详情', NOW(), NOW()),
(106, '提交文章', 3, 'cms', 'article', 'commit', 1, '提交文章', NOW(), NOW()),
(107, '审核文章', 3, 'cms', 'article', 'review', 1, '审核文章', NOW(), NOW()),
(108, '发布文章', 3, 'cms', 'article', 'publish', 1, '发布文章', NOW(), NOW()),
(109, '撤回文章', 3, 'cms', 'article', 'retract', 1, '撤回文章', NOW(), NOW()),
(110, '赞同文章', 3, 'cms', 'article', 'agree', 1, '赞同文章', NOW(), NOW()),
(111, '收藏文章', 3, 'cms', 'article', 'favorite', 1, '收藏文章', NOW(), NOW()),
(112, '转发文章', 3, 'cms', 'article', 'share', 1, '转发文章', NOW(), NOW()),
(113, '评论文章', 3, 'cms', 'article', 'comment', 1, '评论文章', NOW(), NOW()),
(114, '反对文章', 3, 'cms', 'article', 'unagree', 1, '反对文章', NOW(), NOW()),
(150, '取消收藏', 3, 'cms', 'article', 'unfavorite', 1, '取消收藏文章', NOW(), NOW()),
(151, '文章统计', 3, 'cms', 'article', 'tally', 1, '文章统计', NOW(), NOW()),
(152, '重新提交', 3, 'cms', 'article', 'resubmit', 1, '重新提交文章', NOW(), NOW()),
(153, '取消分享', 3, 'cms', 'article', 'unshare', 1, '取消分享文章', NOW(), NOW()),
  (154, '点踩', 3, 'cms', 'article', 'averse', 1, '点踩文章', NOW(), NOW()),
  (155, '取消点踩', 3, 'cms', 'article', 'unaverse', 1, '取消点踩文章', NOW(), NOW()),
  (156, '评论点赞', 3, 'cms', 'comment', 'agree', 1, '点赞评论', NOW(), NOW()),
  (157, '取消评论点赞', 3, 'cms', 'comment', 'unagree', 1, '取消评论点赞', NOW(), NOW()),
  (208, '校验授权', 3, 'system', 'license', 'verify', 1, '校验 license.dat', NOW(), NOW()),
  (209, '授权状态', 3, 'system', 'license', 'status', 1, '查询授权状态', NOW(), NOW());

-- CMS Module Permissions (Comments)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(115, '查看评论', 3, 'cms', 'comment', 'query', 1, '查看评论', NOW(), NOW()),
(116, '创建评论', 3, 'cms', 'comment', 'create', 1, '创建评论', NOW(), NOW()),
(117, '编辑评论', 3, 'cms', 'comment', 'update', 1, '编辑评论', NOW(), NOW()),
(118, '删除评论', 3, 'cms', 'comment', 'delete', 1, '删除评论', NOW(), NOW()),
(119, '评论详情', 3, 'cms', 'comment', 'detail', 1, '评论详情', NOW(), NOW()),
(120, '回复评论', 3, 'cms', 'comment', 'reply', 1, '回复评论', NOW(), NOW());

-- CMS Module Permissions (Sensitive)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(121, '查看敏感词', 3, 'cms', 'sensitive', 'query', 1, '查看敏感词', NOW(), NOW()),
(122, '创建敏感词', 3, 'cms', 'sensitive', 'create', 1, '创建敏感词', NOW(), NOW()),
(123, '编辑敏感词', 3, 'cms', 'sensitive', 'update', 1, '编辑敏感词', NOW(), NOW()),
(124, '删除敏感词', 3, 'cms', 'sensitive', 'delete', 1, '删除敏感词', NOW(), NOW()),
(125, '敏感词详情', 3, 'cms', 'sensitive', 'detail', 1, '查看敏感词详情', NOW(), NOW());

-- CMS Module Permissions (Categories)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(131, '查看分类', 3, 'cms', 'category', 'query', 1, '查看分类', NOW(), NOW()),
(132, '创建分类', 3, 'cms', 'category', 'create', 1, '创建分类', NOW(), NOW()),
(133, '编辑分类', 3, 'cms', 'category', 'update', 1, '编辑分类', NOW(), NOW()),
(134, '删除分类', 3, 'cms', 'category', 'delete', 1, '删除分类', NOW(), NOW()),
(135, '分类详情', 3, 'cms', 'category', 'detail', 1, '查看分类详情', NOW(), NOW());

-- CMS Module Permissions (Terms/Tags)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(141, '查看标签', 3, 'cms', 'term', 'query', 1, '查看标签', NOW(), NOW()),
(142, '创建标签', 3, 'cms', 'term', 'create', 1, '创建标签', NOW(), NOW()),
(143, '编辑标签', 3, 'cms', 'term', 'update', 1, '编辑标签', NOW(), NOW()),
(144, '删除标签', 3, 'cms', 'term', 'delete', 1, '删除标签', NOW(), NOW()),
(145, '标签详情', 3, 'cms', 'term', 'detail', 1, '查看标签详情', NOW(), NOW());

-- CMS Module Permissions (Dashboard)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(161, '查看仪表盘统计', 3, 'cms', 'dashboard', 'query', 1, '查看全平台文章/分类统计与概览', NOW(), NOW()),
(162, '查看用户统计', 3, 'cms', 'dashboard', 'user-stats', 1, '查看全平台用户增长与活跃作者统计（仅管理员）', NOW(), NOW());

-- Monitor Module Permissions
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(201, '查看登录日志', 3, 'monitor', 'login', 'query', 1, '查看登录日志', NOW(), NOW()),
(202, '查看系统监控', 3, 'monitor', 'system', 'query', 1, '查看系统监控', NOW(), NOW()),
(203, '资源统计', 3, 'monitor', 'usage', 'query', 1, '资源统计', NOW(), NOW()),
(206, '查看操作日志', 3, 'monitor', 'journal', 'query', 1, '查看操作日志', NOW(), NOW()),
(207, '管理登录日志', 3, 'monitor', 'login', 'manage', 1, '管理登录日志', NOW(), NOW());

-- Monitor Module Permissions (Cache)
INSERT INTO sys_permission (id, name, type, module, resource, action, status, remark, create_time, update_time) VALUES
(204, '查看缓存', 3, 'monitor', 'cache', 'query', 1, '查看缓存管理', NOW(), NOW()),
(205, '管理缓存', 3, 'monitor', 'cache', 'manage', 1, '清除缓存', NOW(), NOW());

-- ============================================
-- 3. Initialize Role-Permission Associations
-- ============================================
-- ROLE_SYSTEM has all permissions (1-209)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- ROLE_ADMIN: system module + cms module + monitor module
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE module IN ('system', 'cms', 'monitor');

-- ROLE_EDITOR: all cms module permissions (articles, comments, categories, terms, sensitive and notice),
-- except cms:dashboard:user-stats, which counts every user on the platform and stays admin-only.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE (module = 'cms' AND NOT (resource = 'dashboard' AND action = 'user-stats'))
   OR (module = 'system' and resource = 'notice');

-- All non-admin roles: system notice query permission
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE module = 'system' and resource = 'notice';

-- ROLE_USER: cms module (view only + comment permissions)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE module = 'cms'
  AND CONCAT(module, ':', resource, ':', action) IN ('cms:article:query', 'cms:article:create', 'cms:article:update', 'cms:article:delete', 'cms:article:detail', 'cms:article:commit', 'cms:article:resubmit', 'cms:article:agree', 'cms:article:unagree', 'cms:article:favorite', 'cms:article:unfavorite', 'cms:article:unshare', 'cms:article:averse', 'cms:article:unaverse', 'cms:comment:agree', 'cms:comment:unagree', 'cms:article:share', 'cms:article:comment', 'cms:comment:query', 'cms:comment:create', 'cms:comment:update', 'cms:comment:delete', 'cms:comment:detail', 'cms:comment:reply');

-- ============================================
-- 4. Initialize Groups (sys_group)
-- ============================================
INSERT INTO sys_group (id, name, parent_id, level, status, remark, create_time, update_time) VALUES
(1, 'Systems', NULL, 0, 1, '系统管理员组', NOW(), NOW()),
(2, 'Admins', NULL, 0, 1, '管理员组', NOW(), NOW()),
(3, 'Editors', NULL, 0, 1, '内容编辑组', NOW(), NOW()),
(4, 'Users', NULL, 0, 1, '用户组', NOW(), NOW());

-- ============================================
-- 5. Initialize Group-Role Associations
-- ============================================
-- Supers -> ROLE_SYSTEM
INSERT INTO sys_group_role (group_id, role_id) VALUES (1, 1);
-- Admins -> ROLE_ADMIN
INSERT INTO sys_group_role (group_id, role_id) VALUES (2, 2);
-- Editors -> ROLE_EDITOR
INSERT INTO sys_group_role (group_id, role_id) VALUES (3, 3);
-- Users -> ROLE_USER
INSERT INTO sys_group_role (group_id, role_id) VALUES (4, 4);

-- ============================================
-- 6. Initialize Users (sys_user)
-- ============================================
-- SECURITY: Default passwords MUST be changed on first login.
-- These are development-only defaults.
-- Note: Password will be bcrypt hashed
INSERT INTO sys_user (id, nickname, realname, gender, avatar, phone, email, idtype, idcode, motto, birthDate, status, education, location, biography, creator, updater, create_time, update_time) VALUES
	(1, 'system', '管理员', 1, '', '', 'system@ruilink.top', NULL, NULL, '天行健，君子以自强不息', NULL, 1, 0, '610100', '大学之道，在明明德，在亲民，在止于至善。知止而后有定，定而后能静，静而后能安，安而后能虑，虑而后能得。物有本末，事有终始。知所先后，则近道矣。', 1, 1, NOW(), NOW()),
	(2, 'admin', '主管', 1, '', '', 'admin@ruilink.top', NULL, NULL, '地势坤，君子以厚德载物', NULL, 1, NULL, '610400', '夫君子之行，静以修身，俭以养德。非淡泊无以明志，非宁静无以致远。夫学须静也，才须学也，非学无以广才，非志无以成学。淫慢则不能励精，险躁则不能治性。\n年与时驰，意与日去，遂成枯落，多不接世，悲守穷庐，将复何及！', 1, 1, NOW(), NOW());

-- ============================================
-- 7. Initialize Accounts (sys_account)
-- ============================================
-- These accounts MUST have their passwords changed before production use.
-- Credential: bcrypt hash for 'Super99*'
INSERT INTO sys_account (id, user_id, identity, auth_type, credential, status, expiration, login_time, create_time, update_time) VALUES
        (1, 1, 'system', 1, '{"passwordHash":"$2a$12$9lEiHFtspoR02QoDnQT.SuHSrkRlnmqTCvp7/zLTEd1PvRVw/5eG2","type":1}', 1, NULL, NULL, NOW(), NOW()),
	(2, 2, 'admin', 1, '{"passwordHash":"$2a$12$3rczucR4fnPlGrnRi3qRi.N.CabWVXfbJuf/e9zWl8nUk31PbOG2e","type":1}', 1, NULL, NULL, NOW(), NOW());

-- ============================================
-- 8. Initialize Identity Claims (sys_identity)
-- ============================================
-- Verified via ADMIN: seed identity claims for seeded users
INSERT INTO sys_identity (id, user_id, identity_type, identity_value, provider, verified, verified_at, verified_by, status, login_time, creator, updater, create_time, update_time) VALUES
(1, 1, 2, 'system@ruilink.top', NULL, 1, NOW(), 4, 1, NULL, NULL, NULL, NOW(), NOW()),
(2, 2, 2, 'admin@ruilink.top', NULL, 1, NOW(), 4, 1, NULL, NULL, NULL, NOW(), NOW());

-- ============================================
-- 9. Initialize User-Group Associations
-- ============================================
-- super user -> Supers group
INSERT INTO sys_user_group (user_id, group_id) VALUES (1, 1);
-- admin user -> Admins group
INSERT INTO sys_user_group (user_id, group_id) VALUES (2, 2);

-- ============================================
-- 9. Initialize Menus (sys_menu)
-- ============================================
INSERT INTO sys_menu (id, name, title, parent_id, type, icon, path, component, sort, status, authority, create_time, update_time) VALUES
(1, '系统管理', '系统管理', NULL, 4, 'Setting', '/system', 'system/index', 1, 1, 'system:menu:query', NOW(), NOW()),
(2, '用户管理', '用户管理', 1, 1, 'User', '/system/user', 'system/user/index', 1, 1, 'system:user:query', NOW(), NOW()),
(3, '角色管理', '角色管理', 1, 1, 'Avatar', '/system/role', 'system/role/index', 2, 1, 'system:role:query', NOW(), NOW()),
(4, '用户组管理', '用户组管理', 1, 1, 'Management', '/system/group', 'system/group/index', 3, 1, 'system:group:query', NOW(), NOW()),
(5, '菜单管理', '菜单管理', 1, 1, 'Menu', '/system/menu', 'system/menu/index', 4, 1, 'system:menu:query', NOW(), NOW()),
(6, '权限管理', '权限管理', 1, 1, 'Lock', '/system/permission', 'system/permission/index', 5, 1, 'system:permission:query', NOW(), NOW()),
(9, '首页', '首页', NULL, 1, 'HomeFilled', '/home', 'dashboard/index', 0, 1, NULL, NOW(), NOW()),
(10, '内容管理', '内容管理', NULL, 4, 'Document', '/cms', 'cms/index', 2, 1, 'cms:article:query', NOW(), NOW()),
(11, '文章管理', '文章管理', 10, 1, 'Tickets', '/cms/article', 'content/article/index', 1, 1, 'cms:article:query', NOW(), NOW()),
(12, '评论管理', '评论管理', 10, 1, 'Message', '/cms/comment', 'content/comment/index', 2, 1, 'cms:comment:query', NOW(), NOW()),
(13, '敏感词管理', '敏感词管理', 10, 1, 'Warning', '/cms/sensitive', 'content/sensitive/index', 3, 1, 'cms:sensitive:query', NOW(), NOW()),
(14, '分类管理', '分类管理', 10, 1, 'FolderOpened', '/cms/category', 'content/category/index', 4, 1, 'cms:category:query', NOW(), NOW()),
(15, '标签管理', '标签管理', 10, 1, 'PriceTag', '/cms/term', 'content/term/index', 5, 1, 'cms:term:query', NOW(), NOW()),
(20, '系统监控', '系统监控', NULL, 4, 'Monitor', '/monitor', 'monitor/index', 3, 1, 'monitor:login:query', NOW(), NOW()),
(21, '登录日志', '登录日志', 20, 1, 'Right', '/monitor/login-info', 'monitor/login-info/index', 1, 1, 'monitor:login:query', NOW(), NOW()),
(23, '操作日志', '操作日志', 20, 1, 'Notebook', '/monitor/journal', 'monitor/journal/index', 2, 1, 'monitor:journal:query', NOW(), NOW()),
(22, '资源统计', '资源统计', 20, 1, 'DataLine', '/monitor/metrics', 'monitor/metrics/index', 3, 1, 'monitor:system:query', NOW(), NOW()),
(30, '帮助中心', '帮助中心', NULL, 4, 'QuestionFilled', '/help', NULL, 5, 1, NULL, NOW(), NOW()),
(31, '关于系统', '关于系统', 30, 1, 'InfoFilled', '/about', 'about/index', 3, 1, NULL, NOW(), NOW());
