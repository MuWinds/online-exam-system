SET NAMES utf8mb4;

INSERT INTO question
  (id, created_by, question_type, content, standard_answer, score, status)
VALUES
  (11, 1, 'SINGLE_CHOICE', 'Java 中用于定义类的关键字是？', '["A"]', 30.00, 'ACTIVE'),
  (12, 1, 'MULTIPLE_CHOICE', '请选择关系型数据库', '["A","B"]', 40.00, 'ACTIVE'),
  (13, 1, 'JUDGE', '正式成绩应由后端计算。', '["TRUE"]', 30.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  created_by = VALUES(created_by),
  question_type = VALUES(question_type),
  content = VALUES(content),
  standard_answer = VALUES(standard_answer),
  score = VALUES(score),
  status = VALUES(status);

INSERT INTO question_option
  (question_id, option_key, option_content, sort_order)
VALUES
  (11, 'A', 'class', 1),
  (11, 'B', 'def', 2),
  (12, 'A', 'MySQL', 1),
  (12, 'B', 'PostgreSQL', 2),
  (12, 'C', 'Redis', 3),
  (13, 'TRUE', '正确', 1),
  (13, 'FALSE', '错误', 2)
ON DUPLICATE KEY UPDATE
  option_content = VALUES(option_content),
  sort_order = VALUES(sort_order);

INSERT INTO exam_paper
  (id, created_by, paper_name, total_score, status)
VALUES
  (1, 1, '在线考试演示试卷', 100.00, 'PUBLISHED')
ON DUPLICATE KEY UPDATE
  created_by = VALUES(created_by),
  paper_name = VALUES(paper_name),
  total_score = VALUES(total_score),
  status = VALUES(status);

INSERT INTO paper_question
  (paper_id, question_id, sort_order, score)
VALUES
  (1, 11, 1, 30.00),
  (1, 12, 2, 40.00),
  (1, 13, 3, 30.00)
ON DUPLICATE KEY UPDATE
  sort_order = VALUES(sort_order),
  score = VALUES(score);

INSERT INTO exam
  (id, paper_id, created_by, exam_name, start_at, end_at, status, published)
VALUES
  (1, 1, 1, '在线考试演示',
   DATE_SUB(NOW(), INTERVAL 1 HOUR),
   DATE_ADD(NOW(), INTERVAL 2 DAY),
   'ONGOING', TRUE),
  (2, 1, 1, '待开始演示考试',
   DATE_ADD(NOW(), INTERVAL 1 DAY),
   DATE_ADD(NOW(), INTERVAL 3 DAY),
   'NOT_STARTED', TRUE)
ON DUPLICATE KEY UPDATE
  paper_id = VALUES(paper_id),
  created_by = VALUES(created_by),
  exam_name = VALUES(exam_name),
  start_at = VALUES(start_at),
  end_at = VALUES(end_at),
  status = VALUES(status),
  published = VALUES(published);
