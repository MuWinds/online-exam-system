-- Full course schema. Creates missing tables only. Does not clear existing data.
SET NAMES utf8mb4;


-- auth-question.sql
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


-- paper-exam.sql
-- Task2 candidate schema. Confirm with the shared database owner before production migration.
CREATE TABLE IF NOT EXISTS exam_paper (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_by BIGINT NOT NULL,
  paper_name VARCHAR(150) NOT NULL,
  total_score DECIMAL(6,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS paper_question (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  score DECIMAL(6,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_paper_question UNIQUE (paper_id, question_id),
  CONSTRAINT uk_paper_question_order UNIQUE (paper_id, sort_order),
  CONSTRAINT fk_paper_question_paper FOREIGN KEY (paper_id) REFERENCES exam_paper(id),
  CONSTRAINT fk_paper_question_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE TABLE IF NOT EXISTS exam (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  paper_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL,
  exam_name VARCHAR(150) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_exam_paper FOREIGN KEY (paper_id) REFERENCES exam_paper(id)
);


-- answer-score.sql
-- Task3 field proposal. Coordinate with task2 before importing into the shared database.
CREATE TABLE IF NOT EXISTS exam_attempt (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  submission_type VARCHAR(32),
  started_at DATETIME NOT NULL,
  deadline_at DATETIME NOT NULL,
  submitted_at DATETIME,
  paper_snapshot LONGTEXT NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_exam_student UNIQUE (exam_id, student_id)
);
CREATE TABLE IF NOT EXISTS exam_answer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer_content LONGTEXT NOT NULL,
  score DECIMAL(6,2),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_attempt_question UNIQUE (attempt_id,question_id),
  CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt(id)
);
CREATE TABLE IF NOT EXISTS exam_score (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL UNIQUE,
  total_score DECIMAL(6,2) NOT NULL,
  max_score DECIMAL(6,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_score_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt(id)
);

