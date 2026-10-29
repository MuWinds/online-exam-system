-- =====================================================================
-- 任务1：题库（题目、选项）
-- 字段与任务2/任务3已提交的 01-question.sql 保持一致，可直接替换
-- =====================================================================

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
  INDEX idx_question_option_order (question_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
