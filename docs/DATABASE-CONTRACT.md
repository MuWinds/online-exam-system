# 数据库设计与字段提案

## 确认状态

2026-10-28，用户转述全组同意公共规范。公共基线为MySQL 8、utf8mb4、BIGINT主键id、小写下划线命名、DATETIME时间、DECIMAL(6,2)分数。
Word规范列出sys_user、sys_role、question、question_option、exam_paper、paper_question、exam、exam_attempt、exam_answer、exam_score共10张核心表。下表是供各模块评审的候选设计，不代表新增字段、索引、关系、删除策略或枚举已获全组确认；统一标注为待全组确认。
维护边界：任务1负责用户、角色和题库；任务2负责试卷、组卷、考试及总SQL；任务3负责答题和成绩字段提案。正式合并SQL前，各模块需核对字段、外键目标、权限及删除行为。当前分支另提供候选脚本sql/auth-question.sql，仅覆盖任务1表，不替代任务2维护的总SQL。

## 通用设计

- 业务表使用InnoDB、utf8mb4；表名、字段名采用小写下划线。
- 主键候选为BIGINT AUTO_INCREMENT；若团队采用雪花等ID生成策略，需统一调整主键定义。
- 业务表使用created_at、updated_at DATETIME；更新时间由应用更新，或由数据库触发器维护，具体策略待确认。
- 分数字段使用DECIMAL(6,2)。状态字段候选使用VARCHAR，代码层维护枚举；禁止用数据库ENUM锁定仍待确认的枚举。
- 下表的字段长度、空值、默认值、唯一键、索引及外键均为设计提案。跨模块外键需与任务2总SQL和上游实际表结构核对。

## 用户、角色与题库（任务1提案）

### sys_user

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 用户ID | 待全组确认 |
| username | VARCHAR(64) | 非空 | 登录用户名，唯一 | 待全组确认 |
| password_hash | VARCHAR(255) | 非空 | 加盐密码哈希，禁止存储明文 | 待全组确认 |
| real_name | VARCHAR(100) | 非空 | 姓名 | 待全组确认 |
| status | VARCHAR(32) | 非空，候选ACTIVE | 账号状态 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

候选唯一键：username。是否增加学号、工号、邮箱、手机号及逻辑删除字段，由任务1根据登录和账号管理需求确认。

### sys_role

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 角色ID | 待全组确认 |
| role_code | VARCHAR(32) | 非空，唯一 | ADMIN、TEACHER、STUDENT等角色代码 | 待全组确认 |
| role_name | VARCHAR(64) | 非空 | 角色显示名称 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

### 用户角色关系

Word规范未列出用户与角色的关联表。为支持一个用户关联一个或多个角色，候选增加user_role(user_id, role_id)关系表，复合唯一键(user_id, role_id)，分别关联sys_user.id和sys_role.id。若最终业务明确每个账号只能有一个角色，可改为在sys_user存role_id；方案待任务1及全组确认。

### question

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 题目ID | 待全组确认 |
| created_by | BIGINT | 非空 | 创建教师用户ID | 待全组确认 |
| question_type | VARCHAR(32) | 非空 | SINGLE_CHOICE、MULTIPLE_CHOICE或JUDGE | 待全组确认 |
| content | LONGTEXT | 非空 | 题干内容 | 待全组确认 |
| standard_answer | LONGTEXT | 非空 | 服务端标准答案结构 | 待全组确认 |
| analysis | LONGTEXT | 可空 | 答案解析 | 待全组确认 |
| score | DECIMAL(6,2) | 非空 | 题目默认分值 | 待全组确认 |
| status | VARCHAR(32) | 非空，候选ACTIVE | 题目状态 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

候选索引：(created_by, status)、question_type。已被试卷引用的题目不得物理删除；是否采用逻辑删除字段deleted及题目内容/答案的JSON格式待任务1、2确认。标准答案和解析仅允许服务端可信逻辑读取，不得随学生答题试卷返回。

### question_option

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 选项记录ID | 待全组确认 |
| question_id | BIGINT | 非空 | 所属题目 | 待全组确认 |
| option_key | VARCHAR(32) | 非空 | 题目内稳定选项标识，如A | 待全组确认 |
| option_content | LONGTEXT | 非空 | 选项内容 | 待全组确认 |
| sort_order | INT | 非空，候选0 | 展示顺序 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

候选唯一键：(question_id, option_key)；候选外键question_id关联question.id。选项表是否也用于JUDGE题，以及选项是否允许富文本/媒体，待任务1确认。

## 试卷与考试（任务2提案）

### exam_paper

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 试卷ID | 待全组确认 |
| created_by | BIGINT | 非空 | 创建教师用户ID | 待全组确认 |
| paper_name | VARCHAR(150) | 非空 | 试卷名称 | 待全组确认 |
| total_score | DECIMAL(6,2) | 非空，候选0.00 | 试卷总分，等于题目卷面分值之和 | 待全组确认 |
| status | VARCHAR(32) | 非空 | DRAFT、PUBLISHED或ARCHIVED | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

### paper_question

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 关系记录ID | 待全组确认 |
| paper_id | BIGINT | 非空 | 试卷ID | 待全组确认 |
| question_id | BIGINT | 非空 | 题目ID | 待全组确认 |
| sort_order | INT | 非空 | 题目在试卷中的顺序 | 待全组确认 |
| score | DECIMAL(6,2) | 非空 | 本试卷中该题的卷面分值 | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

候选唯一键：(paper_id, question_id)及(paper_id, sort_order)，外键分别关联exam_paper.id和question.id。组卷时是否允许同题重复、是否保存题目内容快照待任务2确认。

### exam

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 主键，自增 | 考试ID | 待全组确认 |
| paper_id | BIGINT | 非空 | 使用的试卷ID | 待全组确认 |
| created_by | BIGINT | 非空 | 发布教师用户ID | 待全组确认 |
| exam_name | VARCHAR(150) | 非空 | 考试名称 | 待全组确认 |
| start_at | DATETIME | 非空 | 考试开始时间 | 待全组确认 |
| end_at | DATETIME | 非空 | 考试结束时间，必须晚于开始时间 | 待全组确认 |
| status | VARCHAR(32) | 非空 | NOT_STARTED、ONGOING或FINISHED | 待全组确认 |
| created_at | DATETIME | 非空，CURRENT_TIMESTAMP | 创建时间 | 待全组确认 |
| updated_at | DATETIME | 非空，CURRENT_TIMESTAMP | 更新时间 | 待全组确认 |

候选索引：(paper_id)、(created_by, status)、(start_at, end_at)。考试状态由发布流程显式更新还是根据时间计算、是否单独配置作答时长、报名与适用班级关系表，均待任务2及全组确认。

### exam补充字段提案

| 字段 | 类型 | 空值/默认 | 用途 | 状态 |
| --- | --- | --- | --- | --- |
| published | BOOLEAN | 非空，FALSE | 区分未发布配置和已发布考试 | 待全组确认 |

当前独立 demo 使用 `published` 区分草稿配置，接口返回时根据北京时间实时计算考试状态。该字段及发布状态规则需任务2与全组确认。

## 答题与成绩（任务3提案）

### exam_attempt

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

### exam_answer

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

### exam_score

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
