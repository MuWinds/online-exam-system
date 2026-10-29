# 任务1 总体设计（登录权限 + 题库管理）

> 与代码、`sql/` 脚本使用同一套术语，字段名可直接在仓库中检索。

## 1. 目标与角色

在线考试管理系统需要覆盖「题库 → 组卷 → 发布考试 → 答题 → 评分 → 统计」的完整流程。任务1负责其中的身份认证与题库建设。

| 角色 | 编码 | 职责 |
| --- | --- | --- |
| 管理员 | `ADMIN` | 管理用户、查看与管理全部题目 |
| 教师 | `TEACHER` | 维护本人创建的题目 |
| 学生 | `STUDENT` | 参加考试、查看成绩（任务3），不访问题库 |

## 2. 技术架构

```text
Vue 3 + Element Plus + Pinia + Axios
        │  Authorization: Bearer <token>
        ▼
Nginx / Vite 代理  /api → Spring Boot 8080
        │
        ├─ AuthInterceptor：解析 JWT，写入 UserContext
        ├─ @RequireRole：接口级角色校验
        ├─ Controller → Service → MyBatis-Plus Mapper
        ▼
MySQL 8（utf8mb4）：sys_role / sys_user / question / question_option
```

## 3. 数据模型

| 表 | 字段 | 说明 |
| --- | --- | --- |
| `sys_role` | id, code, name, description | 角色字典，code 唯一 |
| `sys_user` | id, username, password, real_name, role_id, status, deleted | 用户名唯一，密码 BCrypt |
| `question` | id, created_by, question_type, content, standard_answer, analysis, score, status | 题目，答案存 JSON 数组 |
| `question_option` | id, question_id, option_key, option_content, sort_order | 选项，正确性不在本表 |

说明：`question`、`question_option` 的字段在整合 SQL `sql/auth-question.sql` 中统一维护；登录表结构按当前认证实体使用 `sys_role`、`sys_user`。

### 标准答案格式

| 题型 | question_type | standard_answer | 选项 |
| --- | --- | --- | --- |
| 单选 | `SINGLE_CHOICE` | `["B"]` | A~D |
| 多选 | `MULTIPLE_CHOICE` | `["A","C","D"]`（升序） | A~D |
| 判断 | `JUDGE` | `["TRUE"]` / `["FALSE"]` | TRUE / FALSE 两行 |

答案由服务端校验并写入：选择题的答案必须是选项标号的子集，单选恰好 1 个、多选至少 2 个；判断题由后端固定生成 TRUE / FALSE 两个选项。

## 4. 权限设计

1. **认证**：`POST /api/auth/login` 用 BCrypt 校验密码，签发 2 小时有效的 JWT（含 userId、username、realName、role）。
2. **接口鉴权**：Controller 标注 `@RequireRole({"TEACHER","ADMIN"})`，不满足返回 403。
3. **数据归属**：查询、修改、删除题目时比对 `question.created_by = 当前用户ID`，教师只能操作本人题目；管理员不受限。
4. 前端菜单按角色显示，仅作展示控制，真正的拦截在后端。

## 5. 关键流程

### 5.1 登录

```text
输入账号密码 → AuthService 查询 sys_user
  → BCrypt 校验（失败返回 401，不区分账号/密码错误）
  → status = 1 校验（禁用返回 403）
  → 查询 sys_role → 生成 JWT
  → 返回 { token, tokenType, expiresIn, user }
```

### 5.2 保存题目

```text
提交 questionType / content / score / answer / options
  → 校验题干非空、分值 0.01~100
  → 选择题：2~6 个选项、标号 A~F 不重复、答案 ⊆ 选项
  → 单选题答案数 = 1；多选题答案数 ≥ 2
  → 判断题：固定生成 TRUE/FALSE 选项，答案只能是其一
  → 写入 question（standard_answer 为 JSON），再写 question_option
```

### 5.3 删除题目

```text
校验归属 → 查询 paper_question 引用数（任务2的表）
  → 有引用：返回 409，禁止删除
  → 无引用：先删 question_option（存在外键），再删 question
```

## 6. 与任务2、任务3 的边界

| 编号 | 任务1提供 | 使用方 |
| --- | --- | --- |
| IF-01 | 题目数据与 `standard_answer` 格式 | 任务2组卷、任务3评分 |
| IF-02 | 登录上下文（用户ID、姓名、角色） | 任务2、任务3 |
| IF-03 | 统一返回结构与错误码 | 全部 |
| IF-04 | 角色枚举 `ADMIN` / `TEACHER` / `STUDENT` | 全部 |

任务1不实现：试卷、考试、答题、评分、成绩统计；也不修改 `exam_paper`、`paper_question`、`exam` 等表。
当 `paper_question` 表尚未合并时，删除题目的引用校验会跳过并打印告警，合并后自动生效。
