# API 契约与任务3联调说明

## 确认状态

2026-10-28，用户转述任务1与任务2均对公共确认单无异议。公共标准按 TEAM-CONTRACT-CONFIRMATION.md 执行：/api、数字 code、北京时间、Bearer Token、三种客观题。
本文请求与响应详细字段属于任务3已实现的联调方案，仍需对应成员对照真实模块确认；不表示上游接口已交付。

## 公共格式

成功：`{"code":200,"message":"success","data":{}}`。
失败时 HTTP 状态和 code 一致；400参数错误、401未登录、403越权、404资源不存在、409状态或版本冲突、500服务异常。
日期：yyyy-MM-dd HH:mm:ss，北京时间；Java使用Long的ID在当前JSON中输出数字，前端当前仅支持JS安全整数范围，若团队使用雪花ID需联调改为字符串。
角色：ADMIN、TEACHER、STUDENT。当前用户从 IdentityGateway.resolve 的已验证身份获取，不能由请求体传 userId。

## 已实现接口

| 方法 | 路径 | 权限 | data |
| --- | --- | --- | --- |
| GET | /api/questions | 教师/管理员 | 可用于组卷的题目列表 |
| GET | /api/papers | 教师/管理员 | 当前用户可管理的试卷列表 |
| POST | /api/papers | 教师/管理员 | 创建DRAFT试卷 |
| GET | /api/papers/{id} | 试卷所有者/管理员 | PaperView |
| PUT | /api/papers/{id} | 草稿试卷所有者/管理员 | 更新试卷名称 |
| POST | /api/papers/{id}/questions | 草稿试卷所有者/管理员 | 加入题目并重算总分 |
| DELETE | /api/papers/{id}/questions/{questionId} | 草稿试卷所有者/管理员 | 移除题目并重算总分 |
| POST | /api/papers/{id}/publish | 教师/管理员 | 将非空试卷发布为PUBLISHED |
| POST | /api/papers/{id}/archive | 教师/管理员 | 归档未被考试使用的试卷 |
| GET | /api/managed/exams | 教师/管理员 | 当前用户可管理的考试列表 |
| POST | /api/managed/exams | 教师/管理员 | 创建未发布考试 |
| PUT | /api/managed/exams/{id} | 未发布考试所有者/管理员 | 更新考试配置 |
| POST | /api/managed/exams/{id}/publish | 教师/管理员 | 发布考试 |
| POST | /api/managed/exams/{id}/unpublish | 教师/管理员，未开始 | 撤回考试发布 |
| GET | /api/exams/available | 学生 | 当前已发布且未结束考试 |
| GET | /api/exams/{id} | 已登录用户 | 考试基本信息，学生仅可见已发布考试 |
| POST | /api/exams/{examId}/start | 已报名学生 | Context，首次开始或恢复原答卷 |
| GET | /api/attempts/{id} | 答卷本人 | Context |
| PUT | /api/attempts/{id}/answers | 答卷本人 | 更新后的 Context |
| POST | /api/attempts/{id}/submit | 答卷本人 | Result，重复交卷返回同一成绩 |
| GET | /api/attempts/{id}/result | 答卷本人 | Result，未交卷返回409 |
| GET | /api/scores/my?page=1&size=10 | 学生 | records、total、page、size |
| GET | /api/exams/{examId}/statistics | 本考试教师/管理员 | Statistics |

GET /api/attempts/{id} 是为刷新恢复补充的接口，待全组联调确认。其他上游端点由对应成员实现。

### 任务2管理请求

以下字段、`published`字段和管理路径是本分支为任务2独立演示补充的联调提案，仍需全组确认。

```json
{"paperName":"期中考试试卷"}
```

```json
{"questionId":11,"sortOrder":1,"score":20.00}
```

```json
{
  "paperId":1,
  "examName":"Java程序设计期中考试",
  "startAt":"2026-11-01 09:00:00",
  "endAt":"2026-11-01 11:00:00"
}
```

试卷创建后为DRAFT；空试卷不能发布，发布后不能继续组卷。考试必须引用已发布试卷，结束时间必须晚于开始时间；创建后未发布，发布后根据当前时间计算NOT_STARTED、ONGOING或FINISHED。未开始的已发布考试可以撤回，开始后不能撤回。

## 保存请求

```json
{
  "version": 0,
  "answers": [
    {"questionId": 11, "answer": ["A"]},
    {"questionId": 12, "answer": ["B", "A"]},
    {"questionId": 13, "answer": ["TRUE"]}
  ]
}
```

version为必填，使用最新Context中的值。单选和判断最多一个值，多选按集合判分，空数组清空答案。
本次列表只更新列出的题目；题目重复、无效题目、无效选项或版本冲突时不写入该批答案。交卷后或截止时刻到达后不能再保存。
选项key由上游提供稳定标识；演示判断题使用TRUE/FALSE，真实格式待任务1联调确认。

## DTO 字段

所有详细字段为任务3联调提案，待全组确认。

| DTO | 字段 |
| --- | --- |
| Context | attemptId、examId、examTitle、status、deadlineAt、serverTime、version、questions、answers |
| questions[] | questionId、type、content、options、score |
| options[] | key、label |
| answers | questionId为键、字符串数组为值的对象 |
| Result | attemptId、examId、examTitle、studentId、totalScore、maxScore、submissionType、submittedAt |
| Statistics | participantCount、gradedCount、averageScore、maxScore、minScore、passCount、passRate、distribution |

status结束后为GRADED，submissionType保留SUBMITTED或AUTO_SUBMITTED。后台同步评分在同一事务中完成。
试卷标准答案只保存在服务端快照中，不作为学生响应的一部分。
及格暂按卷面分数60%，passRate为0到100的百分数；distribution以卷面得分百分比分段。
没有已评分记录时平均/最高/最低分为null，及格率0.00。统计暂采用实时聚合，详细口径待全组确认。

## 上游接入

- 任务1实现 IdentityGateway：校验Bearer Token，返回可信Actor。demo凭证不得用于正式鉴权。
- 任务2联合任务1实现 ExamGateway：校验学生报名资格，返回发布时间、考试时间、教师归属、卷面分值和仅服务端可见的标准答案。本分支由 `com.onlineexam.task2.PaperExamService` 提供独立 demo 实现；正式配置必须提供 `ExamEligibilityGateway` 的报名/适用范围实现。
- 开始考试校验已发布和时间；当前实现截止时间为考试结束时间。若团队增加独立作答时长，需共同修改deadline规则。
- 本版只有SINGLE_CHOICE、MULTIPLE_CHOICE、JUDGE；主观题和人工评分留待扩展。
- 公共工程整合时保留团队唯一启动类，导入任务3组件、Mapper和配置，替换演示适配器。

## 待联调事项

上游详细字段、稳定选项key、主键生成策略及JSON大整数、教师权限范围、题目快照策略、及格线来源、独立时长、多次补考和MySQL实机事务行为。
