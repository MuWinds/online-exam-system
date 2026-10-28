-- Demo-only question fixtures. Production question tables are owned by task1.
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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_option (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(32) NOT NULL,
  option_content LONGTEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_demo_question_option UNIQUE (question_id, option_key),
  CONSTRAINT fk_demo_question_option_question FOREIGN KEY (question_id) REFERENCES question(id)
);

MERGE INTO question(id, created_by, question_type, content, standard_answer, score, status)
KEY(id)
VALUES
  (11, 1, 'SINGLE_CHOICE', 'Java 中用于定义类的关键字是？', '["A"]', 30.00, 'ACTIVE'),
  (12, 1, 'MULTIPLE_CHOICE', '请选择关系型数据库', '["A","B"]', 40.00, 'ACTIVE'),
  (13, 1, 'JUDGE', '正式成绩应由后端计算。', '["TRUE"]', 30.00, 'ACTIVE');

MERGE INTO question_option(question_id, option_key, option_content, sort_order)
KEY(question_id, option_key)
VALUES
  (11, 'A', 'class', 1), (11, 'B', 'def', 2),
  (12, 'A', 'MySQL', 1), (12, 'B', 'PostgreSQL', 2), (12, 'C', 'Redis', 3),
  (13, 'TRUE', '正确', 1), (13, 'FALSE', '错误', 2);
