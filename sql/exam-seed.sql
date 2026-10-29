-- Demo-only paper and exam fixtures. Production data is managed through the task2 APIs.
MERGE INTO exam_paper(id, created_by, paper_name, total_score, status)
KEY(id)
VALUES (1, 1, '在线考试演示试卷', 100.00, 'PUBLISHED');
MERGE INTO paper_question(paper_id, question_id, sort_order, score)
KEY(paper_id, question_id)
VALUES (1, 11, 1, 30.00), (1, 12, 2, 40.00), (1, 13, 3, 30.00);

MERGE INTO exam(id, paper_id, created_by, exam_name, start_at, end_at, status, published)
KEY(id)
VALUES
  (1, 1, 1, '在线考试演示', DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
   DATEADD('DAY', 2, CURRENT_TIMESTAMP), 'ONGOING', TRUE),
  (2, 1, 1, '待开始演示考试', DATEADD('DAY', 1, CURRENT_TIMESTAMP),
   DATEADD('DAY', 3, CURRENT_TIMESTAMP), 'NOT_STARTED', TRUE);
