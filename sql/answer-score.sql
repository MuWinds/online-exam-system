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
