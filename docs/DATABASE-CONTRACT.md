# 数据库契约与任务3字段提案

## 确认状态

2026-10-28，用户转述全组同意公共规范。已确认通用标准：MySQL 8、utf8mb4、BIGINT主键id、小写下划线命名、DATETIME时间、DECIMAL(6,2)分数。
任务3表名为exam_attempt、exam_answer、exam_score；任务3提出字段，由任务2合并总SQL。
下面详细字段、长度、默认值、索引和约束仍为任务3实现提案，待任务2及全组联调确认。
脚本：sql/task3.sql。不定义或修改用户、题库、试卷、考试发布等上游表。

## exam_attempt

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 答卷ID | 待全组确认 |
| exam_id | BIGINT | 非空 | 上游考试ID | 待全组确认 |
| student_id | BIGINT | 非空 | 上游用户ID | 待全组确认 |
| status | VARCHAR(32) | 非空 | ANSWERING或GRADED，提交状态在submission_type保留 | 待全组确认 |
| submission_type | VARCHAR(32) | 可空 | SUBMITTED/AUTO_SUBMITTED | 待全组确认 |
| started_at | DATETIME | 非空 | 开始时间 | 待全组确认 |
| deadline_at | DATETIME | 非空 | 服务端截止时间 | 待全组确认 |
| submitted_at | DATETIME | 可空 | 交卷时间，自动交卷记录截止时间 | 待全组确认 |
| paper_snapshot | LONGTEXT | 非空 | 服务端JSON试卷快照，含标准答案，禁止返回学生 | 待全组确认 |
| version | INT | 非空，0 | 保存答案乐观版本号 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间，由更新SQL维护 | 待全组确认 |

唯一键(exam_id,student_id)，本版不支持补考和逻辑删除答卷；保留一份考试记录。自增策略需与团队主键方案对齐。关系和约束待全组确认。

## exam_answer

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 答案记录ID | 待全组确认 |
| attempt_id | BIGINT | 非空 | 答卷ID | 待全组确认 |
| question_id | BIGINT | 非空 | 上游试题ID | 待全组确认 |
| answer_content | LONGTEXT | 非空 | JSON字符串数组，空数组表示未答 | 待全组确认 |
| score | DECIMAL(6,2) | 可空 | 未评分为空，已评分为0或卷面满分 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

唯一键(attempt_id,question_id)，attempt_id引用exam_attempt.id。当前保存通过删除单题旧记录再插入实现，答案记录ID不对外使用，不保留历史版本。关联和策略待全组确认。

## exam_score

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 成绩ID | 待全组确认 |
| attempt_id | BIGINT | 非空，唯一 | 答卷ID | 待全组确认 |
| total_score | DECIMAL(6,2) | 非空 | 后端计算总成绩 | 待全组确认 |
| max_score | DECIMAL(6,2) | 非空 | 卷面总分 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

attempt_id引用exam_attempt.id。exam_id和student_id经答卷关联获取，不重复存储。成绩不接受客户端传入，统计暂实时聚合。关系和策略待全组确认。

## 事务与部署

- 开始考试利用唯一键和幂等插入保证只有一份答卷。
- 答案保存、交卷、评分通过答卷行锁串行化；成绩唯一键保证一份答卷只有一条成绩。
- 答案明细、成绩和GRADED状态在一个事务中落库。
- 定时扫描截止时间已到的ANSWERING记录，重启后继续处理逾期记录。
- MySQL使用InnoDB和北京时间；数据库不得向前端直接暴露。
- demo使用H2 MySQL兼容模式，仅用于独立演示；重启会清空。
- MySQL环境由任务2提供，导入脚本前先核对已有表结构；IF NOT EXISTS不执行结构迁移，不应掩盖字段不一致。
