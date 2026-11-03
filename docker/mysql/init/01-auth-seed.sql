SET NAMES utf8mb4;

INSERT INTO sys_role (id, code, name, description)
VALUES
  (1, 'ADMIN', '管理员', '本地演示'),
  (2, 'TEACHER', '教师', '本地演示'),
  (3, 'STUDENT', '学生', '本地演示')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO sys_user (id, username, password, real_name, role_id, status, deleted)
VALUES
  (1, 'teacher', 'pending-docker-seed', '演示教师', 2, 1, 0),
  (2, 'admin', 'pending-docker-seed', '演示管理员', 1, 1, 0),
  (100, 'student1', 'pending-docker-seed', '演示学生一', 3, 1, 0),
  (101, 'student2', 'pending-docker-seed', '演示学生二', 3, 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), role_id = VALUES(role_id), status = 1, deleted = 0;
