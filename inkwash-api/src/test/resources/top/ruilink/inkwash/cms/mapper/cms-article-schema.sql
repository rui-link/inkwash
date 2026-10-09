DROP TABLE IF EXISTS cms_article;

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