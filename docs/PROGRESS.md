# 项目整合进度

最近更新：2026-10-29。当前分支：develop。

## 整合结构

三个任务的代码已并入唯一应用入口，根目录 `backend` 为唯一 Maven 工程，启动类为 `com.onlineexam.OnlineExamApplication`；根目录 `frontend` 为唯一 Vue 应用。

后端业务按职责划分为 `auth`（登录、用户与角色）、`question`（题库）、`paper`（试卷、组卷与考试发布）、`answer`（答题、评分与成绩）。共享的身份上下文、统一响应/异常和基础配置位于 `common` 与 `config`。题库统一使用 `/api/questions`；试卷模块的候选题目接口为 `/api/paper-questions`，避免路由冲突。`paper` 实现 `ExamGateway` 所需的试卷快照读取，答题模块通过 `IdentityGateway` 复用已验证的 JWT 用户身份。

## 模块与适配器

- 认证使用 JWT、角色和用户数据；题库保留教师归属校验及标准答案服务端访问。
- 试卷/考试管理包括草稿组卷、发布、归档、考试配置和发布。
- 答题/成绩包括恢复、保存、幂等交卷、客观题自动评分、个人成绩和考试统计。
- `demo` profile 保留任务2/3服务测试用的演示适配器；`docker` profile 使用统一登录账号，演示报名资格仅允许两个演示学生。
- Docker 演示账号由初始化 SQL 先建立身份关系，应用启动时用 BCrypt 设置演示密码。不要将演示密码或默认 JWT 密钥用于共享/生产环境。
- Docker 中文演示数据初始化显式使用 `utf8mb4`；已有数据卷中的角色、题目、选项、试卷与考试乱码由 `docker/mysql/migrations/20261029-fix-demo-question-encoding.sql` 按原始字节条件修复。

## 验证状态

| 项目 | 结果 | 备注 |
| --- | --- | --- |
| 后端编译 | 通过 | JDK 21，Maven `verify` 编译目标 release 17 |
| 答题服务测试 | 10 项通过 | H2 MySQL 兼容模式 |
| 答题 API 测试 | 3 项通过 | demo 身份、响应字段和错误格式 |
| 试卷服务测试 | 3 项通过 | 所有者权限、组卷和发布约束 |
| 试卷 API 测试 | 1 项通过 | 教师发布考试、学生可见并进入答题 |
| 后端完整回归 | 17 项通过 | `mvn -f backend/pom.xml verify` |
| 前端状态测试 | 3 项通过 | 保存队列、倒计时和清空答案 |
| 前端生产构建 | 通过 | Vite 有 Element Plus 主包体积提示 |
| Docker 镜像构建与容器启动 | 通过 | Compose 构建成功；MySQL healthy，前后端容器运行，后端 Java 17 已连接 MySQL |
| Docker 登录与答题 API | 通过 | admin、teacher、student1 均以页面演示密码登录成功；答题、幂等交卷和教师统计通过，演示成绩100分 |
| Docker 演示数据中文编码 | 通过 | 角色、题目、选项、试卷与考试名称及 API 返回值均核验正常 |

## 待确认/待验收

- MySQL 现有数据库若已初始化，不会重跑 Docker init SQL；本次在新建数据卷验证，仍需确认旧库最终合并表结构和必要迁移。
- 报名与适用范围规则目前由 demo/docker 适配器提供，正式考试需接入实际资格规则。
- SQL 字段、接口 DTO、用户/题目数据结构仍以 `docs/API-CONTRACT.md`、`docs/DATABASE-CONTRACT.md` 标注的全组评审状态为准。
- 完成 MySQL 并发/事务回滚、真实任务账号权限边界及浏览器端人工流程验收后，再将整体系统测试标记完成。
