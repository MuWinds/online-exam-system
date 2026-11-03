-- Candidate schema for task 1 authentication and question-bank tables.
-- Review with task 2 before merging into the shared database script.
-- This script intentionally does not create paper, exam, attempt, answer,
-- or score tables.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_role_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(100) NOT NULL,
  role_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_user_username UNIQUE (username),
  CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_by BIGINT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  content LONGTEXT NOT NULL,
  standard_answer LONGTEXT NOT NULL,
  analysis LONGTEXT,
  score DECIMAL(6,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_question_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT ck_question_score_positive CHECK (score > 0),
  INDEX idx_question_creator_status (created_by, status),
  INDEX idx_question_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_option (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(32) NOT NULL,
  option_content LONGTEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_question_option_key UNIQUE (question_id, option_key),
  CONSTRAINT fk_question_option_question FOREIGN KEY (question_id) REFERENCES question(id),
  CONSTRAINT ck_question_option_sort_order_nonnegative CHECK (sort_order >= 0),
  INDEX idx_question_option_order (question_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
