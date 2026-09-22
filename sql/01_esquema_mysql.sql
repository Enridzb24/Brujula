CREATE DATABASE IF NOT EXISTS brujula_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE brujula_db;

CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 email VARCHAR(160) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(10) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS categories (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 name VARCHAR(60) NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK (kind IN ('INGRESO','GASTO')),
 CONSTRAINT fk_category_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT uq_category UNIQUE(user_id,name,kind),
 CONSTRAINT uq_category_owner UNIQUE(id,user_id)
);
CREATE TABLE IF NOT EXISTS movements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 description VARCHAR(160) NOT NULL,
 amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
 movement_date DATE NOT NULL,
 payment_method VARCHAR(30) NOT NULL,
 CONSTRAINT fk_movement_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_movement_category FOREIGN KEY(category_id,user_id) REFERENCES categories(id,user_id),
 INDEX idx_movements_user_date (user_id,movement_date)
);
CREATE TABLE IF NOT EXISTS budgets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 category_id BIGINT NOT NULL,
 month_key VARCHAR(7) NOT NULL,
 amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
 CONSTRAINT fk_budget_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_budget_category FOREIGN KEY(category_id,user_id) REFERENCES categories(id,user_id),
 CONSTRAINT uq_budget UNIQUE(user_id,category_id,month_key)
);
CREATE TABLE IF NOT EXISTS goals (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 name VARCHAR(80) NOT NULL,
 target DECIMAL(12,2) NOT NULL CHECK (target > 0),
 saved DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (saved >= 0 AND saved <= target),
 deadline DATE NOT NULL,
 CONSTRAINT fk_goal_user FOREIGN KEY(user_id) REFERENCES users(id)
);
