# 任务1 接口文档

> Base URL：`http://localhost:8080/api`
> 鉴权：`Authorization: Bearer <token>`
> 时间格式：`yyyy-MM-dd HH:mm:ss`（Asia/Shanghai）
> 统一返回：`{ "code": 200, "message": "success", "data": {} }`

## 1. 状态码

| code | 含义 | 场景 |
| --- | --- | --- |
| 200 | 成功 | 查询、保存、删除成功 |
| 400 | 参数错误 | 字段缺失、答案与选项不匹配 |
| 401 | 未登录 | Token 缺失、格式错误或过期 |
| 403 | 无权限 | 学生访问题库、教师操作他人题目、账号禁用 |
| 404 | 资源不存在 | 题目 ID 不存在 |
| 409 | 业务冲突 | 用户名重复、题目被试卷引用 |
| 500 | 系统错误 | 未捕获异常 |

## 2. 登录

### POST /api/auth/login（匿名）

```json
{ "username": "teacher", "password": "123456" }
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "teacher",
      "realName": "张老师",
      "roleCode": "TEACHER",
      "roleName": "教师",
      "status": 1,
      "createdAt": "2026-10-29 10:00:00"
    }
  }
}
```

### GET /api/auth/current-user（已登录）

返回同上 `user` 对象。

### POST /api/auth/logout（已登录）

JWT 无状态，服务端不保存会话，前端清除 Token 即可。

### PUT /api/auth/password（已登录）

```json
{ "oldPassword": "123456", "newPassword": "abc12345" }
```

## 3. 用户管理（ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/users?page=1&size=10&keyword=&roleCode=` | 分页查询 |
| POST | `/api/users` | 新增用户 |
| PUT | `/api/users/{id}` | 修改姓名、角色、状态 |
| PUT | `/api/users/{id}/password` | 重置密码 |
| GET | `/api/roles` | 角色下拉 |

新增用户：

```json
{ "username": "teacher3", "password": "123456", "realName": "李老师", "roleCode": "TEACHER", "status": 1 }
```

`password` 可省略，默认使用 `app.default-password`（123456）。

## 4. 题库管理（ADMIN / TEACHER）

### 4.1 分页查询

`GET /api/questions?page=1&size=10&questionType=SINGLE_CHOICE&keyword=事务`

- 教师只能查到本人 `created_by` 的题目，管理员可查全部
- 列表不返回 `options`

```json
{
  "id": 4,
  "questionType": "SINGLE_CHOICE",
  "typeName": "单选题",
  "content": "数据库事务的特性不包括（  ）。",
  "standardAnswer": ["D"],
  "analysis": "事务的四个特性为原子性、一致性、隔离性和持久性。",
  "score": 2.00,
  "createdBy": 1,
  "creatorName": "张老师",
  "status": "ACTIVE",
  "createdAt": "2026-10-29 10:00:00",
  "updatedAt": "2026-10-29 10:00:00",
  "options": null
}
```

### 4.2 题目详情

`GET /api/questions/{id}`，比列表多返回 `options`：

```json
"options": [
  { "id": 9, "optionKey": "A", "optionContent": "原子性", "sortOrder": 1 },
  { "id": 10, "optionKey": "B", "optionContent": "一致性", "sortOrder": 2 },
  { "id": 11, "optionKey": "C", "optionContent": "隔离性", "sortOrder": 3 },
  { "id": 12, "optionKey": "D", "optionContent": "可扩展性", "sortOrder": 4 }
]
```

### 4.3 新增 / 修改

- 新增：`POST /api/questions`，返回新题目 ID
- 修改：`PUT /api/questions/{id}`

单选题：

```json
{
  "questionType": "SINGLE_CHOICE",
  "content": "下列哪个是关系型数据库？",
  "score": 2,
  "analysis": "MySQL 是关系型数据库。",
  "answer": ["A"],
  "options": [
    { "optionKey": "A", "content": "MySQL" },
    { "optionKey": "B", "content": "Redis" },
    { "optionKey": "C", "content": "MongoDB" },
    { "optionKey": "D", "content": "HBase" }
  ]
}
```

判断题（不传 options）：

```json
{
  "questionType": "JUDGE",
  "content": "HTTP 协议本身是无状态的。",
  "score": 2,
  "answer": ["TRUE"]
}
```

服务端会为判断题自动生成 `TRUE` / `FALSE` 两个选项。

校验规则：

| 校验项 | 规则 |
| --- | --- |
| 题干 | 非空，≤1000 字 |
| 分值 | 0.01 ~ 100 |
| 选项数 | 2 ~ 6 |
| 选项标号 | A~F，不可重复 |
| 答案 | 必须属于选项标号；单选恰好 1 个，多选 ≥ 2 个 |
| 判断题答案 | 只能是 TRUE / FALSE |

### 4.4 删除

`DELETE /api/questions/{id}`

先检查 `paper_question` 引用，被引用时返回：

```json
{ "code": 409, "message": "该题目已被 1 份试卷引用，不能删除", "data": null }
```

未被引用时先删选项再删题目（`question_option` 对 `question` 有外键）。
