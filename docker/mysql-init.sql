CREATE DATABASE IF NOT EXISTS demodb;
USE demodb;

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    age INT NOT NULL
    );

INSERT INTO users (name, age) VALUES
                                  ('Nguyễn Văn An', 25),
                                  ('Trần Thị Bình', 30),
                                  ('Lê Văn Cường', 22),
                                  ('Phạm Thị Dung', 28),
                                  ('Hoàng Văn Em', 35);
