-- H2 Database Schema for inkwash (Dev Environment)
-- Compatible with MySQL mode

-- Drop tables if exists (in reverse order)
DROP TABLE IF EXISTS cms_article_term;
DROP TABLE IF EXISTS cms_sensitive;
DROP TABLE IF EXISTS cms_interaction;
DROP TABLE IF EXISTS cms_comment;
DROP TABLE IF EXISTS cms_term;
DROP TABLE IF EXISTS cms_category;
DROP TABLE IF EXISTS cms_article;
DROP TABLE IF EXISTS media_file;
DROP TABLE IF EXISTS sys_preference;
DROP TABLE IF EXISTS mon_journal;
DROP TABLE IF EXISTS mon_login_info;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_group;
DROP TABLE IF EXISTS sys_group_role;
DROP TABLE IF EXISTS sys_identity;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_account;
DROP TABLE IF EXISTS sys_group;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_menu;

-- Create sys_role table
CREATE TABLE sys_role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    code VARCHAR(60) NOT NULL UNIQUE,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sys_permission table
CREATE TABLE sys_permission (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    type TINYINT DEFAULT 3,
    module VARCHAR(60),
    resource VARCHAR(60),
    action VARCHAR(60),
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sys_role_permission join table (role -> permission mapping)
CREATE TABLE sys_role_permission (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

-- Create sys_account table
CREATE TABLE sys_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    identity VARCHAR(120) NOT NULL,
    auth_type TINYINT DEFAULT 1,
    credential TEXT,
    status TINYINT DEFAULT 1,
    expiration INT,
    login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_identity_type (identity, auth_type)
);

-- Create sys_identity table (verified identity claims)
CREATE TABLE sys_identity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    identity_type TINYINT NOT NULL COMMENT '1=PHONE 2=EMAIL 3=OIDC_SUB',
    identity_value VARCHAR(120) NOT NULL,
    provider VARCHAR(30),
    verified TINYINT DEFAULT 0,
    verified_at TIMESTAMP,
    verified_by TINYINT COMMENT '1=USER 2=SMS_CODE 3=OAUTH2 4=ADMIN',
    status TINYINT DEFAULT 1,
    login_time TIMESTAMP,
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_identity_type_value UNIQUE (identity_type, identity_value),
    CONSTRAINT uk_identity_type_provider_value UNIQUE (identity_type, provider, identity_value)
);
CREATE INDEX idx_identity_user_id ON sys_identity(user_id);

-- Create sys_user table
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(60),
    realname VARCHAR(60),
    gender TINYINT DEFAULT 0,
    avatar VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(120),
    idtype TINYINT,
    idcode VARCHAR(60),
    motto VARCHAR(120),
    birthDate TIMESTAMP,
    status TINYINT DEFAULT 1,
    education TINYINT DEFAULT 1,
    location VARCHAR(60),
    biography VARCHAR(500),
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sys_group table
CREATE TABLE sys_group (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    parent_id INT,
    level INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create sys_group_role join table
CREATE TABLE sys_group_role (
    group_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (group_id, role_id)
);

-- Create sys_user_group join table
CREATE TABLE sys_user_group (
    user_id BIGINT NOT NULL,
    group_id INT NOT NULL,
    PRIMARY KEY (user_id, group_id)
);

-- Create sys_menu table
CREATE TABLE sys_menu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    title VARCHAR(60),
    parent_id INT,
    type TINYINT DEFAULT 1,
    icon VARCHAR(60),
    path VARCHAR(240),
    component VARCHAR(240),
    visible TINYINT DEFAULT 1,
    redirect VARCHAR(240),
    tree_path VARCHAR(500),
    keep_alive TINYINT DEFAULT 0,
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    authority VARCHAR(120),
    remark VARCHAR(500),
    creator BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System notice table
CREATE TABLE sys_notice (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    type TINYINT DEFAULT 1,
    content TEXT,
    status TINYINT DEFAULT 1,
    recipient_id BIGINT NULL,
    read_time TIMESTAMP NULL,
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Monitor Module Tables
-- ============================================

-- Login info table
CREATE TABLE mon_login_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    identity VARCHAR(120),
    login_type VARCHAR(20),
    address VARCHAR(60),
    device VARCHAR(120),
    browser VARCHAR(60),
    ostype VARCHAR(60),
    status TINYINT DEFAULT 1,
    message VARCHAR(240),
login_time TIMESTAMP,
      logout_time TIMESTAMP,
      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creator BIGINT,
    updater BIGINT
);

-- Journal table (operation audit)
CREATE TABLE mon_journal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    user_name VARCHAR(100),
    module VARCHAR(100),
    operation VARCHAR(200),
    url VARCHAR(500),
    method VARCHAR(200),
    param TEXT,
    result TINYINT DEFAULT 1,
    fail_reason TEXT,
    ip VARCHAR(45),
    duration BIGINT DEFAULT 0,
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_journal_user ON mon_journal(user_id);
CREATE INDEX idx_journal_time ON mon_journal(create_time);

-- File storage table
CREATE TABLE media_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    folder_type INT NOT NULL DEFAULT 1,
    original_name VARCHAR(255),
    file_key VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User preference table
CREATE TABLE sys_preference (
    user_id BIGINT PRIMARY KEY,
    theme VARCHAR(20) DEFAULT 'sky-blue',
    language VARCHAR(10) DEFAULT 'zh-CN',
    menu_style VARCHAR(20) DEFAULT 'left',
    options TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CMS Module Tables
-- ============================================

-- Article table
CREATE TABLE cms_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(240) NOT NULL,
    slug VARCHAR(240),
    summary VARCHAR(500),
    cover_url VARCHAR(500),
    content TEXT,
    content_type VARCHAR(20) DEFAULT 'markdown',
    author_id BIGINT,
    category_id INT,
    status TINYINT DEFAULT 1,
    reviewer_id BIGINT,
    opinion VARCHAR(240),
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    agree_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    averse_count INT DEFAULT 0,
    publish_time TIMESTAMP,
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Article category table
CREATE TABLE cms_category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    parent_id INT,
    name VARCHAR(60) NOT NULL,
    slug VARCHAR(60),
    remark VARCHAR(240),
    level INT DEFAULT 1,
    status TINYINT DEFAULT 1,
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

-- Comment table
CREATE TABLE cms_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT,
    commenter_id BIGINT,
    parent_id BIGINT,
    content TEXT,
    agree_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Interaction table (点赞、收藏、转 ?
CREATE TABLE cms_interaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    agree BOOLEAN DEFAULT FALSE,
    favorite BOOLEAN DEFAULT FALSE,
    share BOOLEAN DEFAULT FALSE,
    averse BOOLEAN DEFAULT FALSE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_actor (article_id, actor_id)
  );

  -- Comment interaction table (mirrors cms_interaction; scoped to comments, not articles)
  CREATE TABLE cms_comment_interaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    agree BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_comment_actor (comment_id, actor_id)
  );

-- Term table
CREATE TABLE cms_term (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    slug VARCHAR(60),
    status TINYINT DEFAULT 1,
    remark VARCHAR(240),
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Article tag relation table
CREATE TABLE cms_article_term (
    article_id BIGINT NOT NULL,
    term_id INT NOT NULL,
    PRIMARY KEY (article_id, term_id)
);

-- Sensitive word table
CREATE TABLE cms_sensitive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(120) NOT NULL,
    type TINYINT DEFAULT 1,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    creator BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_menu_parent ON sys_menu(parent_id);
CREATE INDEX idx_actor_favorite_time ON cms_interaction(actor_id, favorite, create_time);
CREATE INDEX idx_actor_agree_time ON cms_interaction(actor_id, agree, create_time);
CREATE INDEX idx_actor_averse_time ON cms_interaction(actor_id, averse, create_time);
CREATE INDEX idx_comment_user ON cms_comment(commenter_id);

CREATE INDEX idx_article_status_publish ON cms_article(status, publish_time);
CREATE INDEX idx_article_author_created ON cms_article(author_id, create_time);
CREATE INDEX idx_article_status_created ON cms_article(status, create_time);
CREATE INDEX idx_article_category ON cms_article(category_id);
CREATE INDEX idx_comment_article ON cms_comment(article_id);
