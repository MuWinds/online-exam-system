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
