DROP TABLE IF EXISTS sys_user;

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
