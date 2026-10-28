# API 契约草案

> 状态：**待全组确认**
>
> 本文仅用于任务拆分和联调前讨论，不代表最终接口。所有公共端点、参数、字段、错误码、权限和业务规则均须全组确认后实施。

## 1. 暂定基础约定

| 项目 | 候选约定 | 状态 |
| --- | --- | --- |
| API 前缀 | `/api/v1` | 待全组确认 |
| 数据格式 | `application/json; charset=UTF-8` | 待全组确认 |
| 鉴权方式 | `Authorization: Bearer <token>` | 待全组确认 |
| 时间格式 | ISO 8601，例如 `2026-10-28T20:00:00+08:00` | 待全组确认 |
| 分页参数 | `page` 从 1 开始，`size` 表示每页数量 | 待全组确认 |
| 分数格式 | 十进制定点数，建议保留 2 位小数 | 待全组确认 |
| 幂等策略 | 交卷接口按答卷 ID 保证幂等 | 待全组确认 |

## 2. 公共响应结构

以下字段全部为候选公共字段。

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `code` | string | 业务状态码 | 待全组确认 |
| `message` | string | 面向调用方的结果说明 | 待全组确认 |
| `data` | object / array / null | 业务数据 | 待全组确认 |
| `traceId` | string | 请求追踪标识 | 待全组确认 |
| `timestamp` | string | 服务端响应时间 | 待全组确认 |

候选示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "candidate-trace-id",
  "timestamp": "2026-10-28T20:00:00+08:00"
}
```

## 3. 在线答题接口

| 方法 | 候选路径 | 用途 | 候选权限 | 状态 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/exams/{examId}/attempts` | 创建或恢复一次答题 | 考生 | 待全组确认 |
| `GET` | `/api/v1/attempts/{attemptId}` | 获取答题上下文与已保存答案 | 考生本人 | 待全组确认 |
| `PUT` | `/api/v1/attempts/{attemptId}/answers/{questionId}` | 保存或更新单题答案 | 考生本人 | 待全组确认 |
| `POST` | `/api/v1/attempts/{attemptId}/submit` | 提交答卷并触发评分 | 考生本人 | 待全组确认 |

### 3.1 创建或恢复答题

候选请求字段：

| 字段 | 候选类型 | 必填 | 说明 | 状态 |
| --- | --- | --- | --- | --- |
| `examId` | long（路径参数） | 是 | 考试 ID | 待全组确认 |
| `clientRequestId` | string | 否 | 客户端幂等请求标识 | 待全组确认 |

候选响应字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `attemptId` | long | 答卷 ID | 待全组确认 |
| `examId` | long | 考试 ID | 待全组确认 |
| `status` | string | `IN_PROGRESS`、`SUBMITTED` 等候选状态 | 待全组确认 |
| `startedAt` | string | 开始时间 | 待全组确认 |
| `deadlineAt` | string | 本次作答截止时间 | 待全组确认 |
| `remainingSeconds` | long | 服务端计算的剩余秒数 | 待全组确认 |

### 3.2 获取答题上下文

候选响应字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `attemptId` | long | 答卷 ID | 待全组确认 |
| `examTitle` | string | 考试标题 | 待全组确认 |
| `status` | string | 当前答题状态 | 待全组确认 |
| `deadlineAt` | string | 截止时间 | 待全组确认 |
| `questions` | array | 题目列表，不应包含标准答案 | 待全组确认 |
| `answers` | array | 已保存答案 | 待全组确认 |

候选 `questions[]` 字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `questionId` | long | 试题 ID | 待全组确认 |
| `type` | string | 单选、多选、判断、填空、简答等 | 待全组确认 |
| `content` | string | 题干 | 待全组确认 |
| `options` | array | 选项；非选择题可为空 | 待全组确认 |
| `score` | decimal | 题目分值 | 待全组确认 |
| `orderNo` | integer | 展示顺序 | 待全组确认 |

### 3.3 保存单题答案

候选请求字段：

| 字段 | 候选类型 | 必填 | 说明 | 状态 |
| --- | --- | --- | --- | --- |
| `attemptId` | long（路径参数） | 是 | 答卷 ID | 待全组确认 |
| `questionId` | long（路径参数） | 是 | 试题 ID | 待全组确认 |
| `answer` | string / array / object | 是 | 按题型保存的作答内容 | 待全组确认 |
| `clientUpdatedAt` | string | 否 | 客户端最后修改时间 | 待全组确认 |
| `version` | integer | 否 | 乐观锁版本号 | 待全组确认 |

候选响应字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `savedAt` | string | 服务端保存时间 | 待全组确认 |
| `version` | integer | 最新版本号 | 待全组确认 |

### 3.4 提交答卷

候选请求字段：

| 字段 | 候选类型 | 必填 | 说明 | 状态 |
| --- | --- | --- | --- | --- |
| `attemptId` | long（路径参数） | 是 | 答卷 ID | 待全组确认 |
| `clientRequestId` | string | 否 | 防止重复提交的幂等标识 | 待全组确认 |

候选响应字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `attemptId` | long | 答卷 ID | 待全组确认 |
| `status` | string | `SUBMITTED`、`AUTO_GRADED`、`PENDING_REVIEW` 等 | 待全组确认 |
| `submittedAt` | string | 实际交卷时间 | 待全组确认 |
| `objectiveScore` | decimal / null | 客观题得分 | 待全组确认 |
| `totalScore` | decimal / null | 总分；待人工评分时可为空 | 待全组确认 |

## 4. 评分接口

| 方法 | 候选路径 | 用途 | 候选权限 | 状态 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/attempts/{attemptId}/grade` | 触发或重试自动评分 | 教师/管理员或内部服务 | 待全组确认 |
| `PUT` | `/api/v1/attempts/{attemptId}/answers/{questionId}/score` | 录入主观题得分 | 教师/管理员 | 待全组确认 |
| `POST` | `/api/v1/attempts/{attemptId}/finalize-score` | 汇总并发布最终成绩 | 教师/管理员 | 待全组确认 |

候选人工评分请求字段：

| 字段 | 候选类型 | 必填 | 说明 | 状态 |
| --- | --- | --- | --- | --- |
| `score` | decimal | 是 | 本题得分 | 待全组确认 |
| `comment` | string | 否 | 评语 | 待全组确认 |
| `version` | integer | 否 | 防止并发覆盖 | 待全组确认 |

## 5. 成绩查询接口

| 方法 | 候选路径 | 用途 | 候选权限 | 状态 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/me/results` | 查询当前考生的成绩列表 | 考生 | 待全组确认 |
| `GET` | `/api/v1/results/{resultId}` | 查询单次成绩详情 | 本人/教师/管理员 | 待全组确认 |
| `GET` | `/api/v1/exams/{examId}/results` | 分页查询某考试成绩 | 教师/管理员 | 待全组确认 |

候选成绩字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `resultId` | long | 成绩记录 ID | 待全组确认 |
| `examId` | long | 考试 ID | 待全组确认 |
| `examTitle` | string | 考试标题 | 待全组确认 |
| `userId` | long | 考生 ID | 待全组确认 |
| `objectiveScore` | decimal | 客观题得分 | 待全组确认 |
| `subjectiveScore` | decimal | 主观题得分 | 待全组确认 |
| `totalScore` | decimal | 总分 | 待全组确认 |
| `status` | string | 评分或发布状态 | 待全组确认 |
| `submittedAt` | string | 交卷时间 | 待全组确认 |
| `publishedAt` | string / null | 成绩发布时间 | 待全组确认 |

## 6. 成绩统计接口

| 方法 | 候选路径 | 用途 | 候选权限 | 状态 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/exams/{examId}/statistics` | 获取考试总体统计 | 教师/管理员 | 待全组确认 |
| `GET` | `/api/v1/exams/{examId}/statistics/distribution` | 获取分数段分布 | 教师/管理员 | 待全组确认 |
| `GET` | `/api/v1/exams/{examId}/statistics/questions` | 获取逐题统计 | 教师/管理员 | 待全组确认 |

候选总体统计字段：

| 字段 | 候选类型 | 说明 | 状态 |
| --- | --- | --- | --- |
| `participantCount` | long | 参与人数 | 待全组确认 |
| `submittedCount` | long | 已交卷人数 | 待全组确认 |
| `gradedCount` | long | 已完成评分人数 | 待全组确认 |
| `averageScore` | decimal | 平均分 | 待全组确认 |
| `maxScore` | decimal | 最高分 | 待全组确认 |
| `minScore` | decimal | 最低分 | 待全组确认 |
| `passCount` | long | 及格人数 | 待全组确认 |
| `passRate` | decimal | 及格率 | 待全组确认 |
| `distribution` | array | 分数段分布 | 待全组确认 |

## 7. 候选错误码

| HTTP 状态 | 候选业务码 | 场景 | 状态 |
| --- | --- | --- | --- |
| `400` | `INVALID_ARGUMENT` | 参数格式或业务参数不合法 | 待全组确认 |
| `401` | `UNAUTHORIZED` | 未登录或凭证失效 | 待全组确认 |
| `403` | `FORBIDDEN` | 无权访问答卷或成绩 | 待全组确认 |
| `404` | `EXAM_NOT_FOUND` | 考试不存在 | 待全组确认 |
| `404` | `ATTEMPT_NOT_FOUND` | 答卷不存在 | 待全组确认 |
| `409` | `ATTEMPT_ALREADY_SUBMITTED` | 已交卷后继续作答 | 待全组确认 |
| `409` | `ANSWER_VERSION_CONFLICT` | 答案版本冲突 | 待全组确认 |
| `422` | `EXAM_NOT_AVAILABLE` | 未到开考时间或已结束 | 待全组确认 |
| `500` | `INTERNAL_ERROR` | 未分类服务端错误 | 待全组确认 |

## 8. 全组评审清单

- 用户、考试、试卷、试题和答卷 ID 的类型与命名。
- 鉴权方案、角色名称与数据权限边界。
- 答案的统一存储格式以及各题型编码。
- 自动保存频率、版本冲突处理与断网恢复策略。
- 交卷幂等、超时自动交卷和迟交规则。
- 客观题评分规则，多选题是否允许部分得分。
- 主观题评分及成绩发布流程。
- 分页、排序、时间、分数精度、错误码和公共响应格式。
- 成绩统计口径及未完成评分记录是否纳入统计。
