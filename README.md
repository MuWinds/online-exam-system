# 在线考试管理系统

软件工程课程设计。根目录 `backend` 与 `frontend` 是唯一构建和运行入口，按认证、题库、试卷考试、答题成绩划分业务模块。

系统包含 JWT 登录与用户权限、题库管理、试卷组卷与考试发布、在线答题、自动评分、成绩查询和统计。试卷和答题模块通过明确的业务包和网关接口协作；`/api/questions` 由题库模块提供，组卷候选题目列表使用 `/api/paper-questions`。

## 环境

- Java 17及以上、Maven 3.9；编译目标Java17。
- 前端团队约定Node20，当前验证环境Node24。
- Spring Boot3.3.5、MyBatis-Plus3.5.5；前端Vue3、Element Plus、Axios、Vite。具体补丁版本属于本次独立模块选取，需与公共工程对齐。

## 本地演示

仓库根目录执行 `docker compose up --build`，然后访问 http://127.0.0.1:8088。演示账号为 `teacher`、`admin`、`student1`、`student2`，默认密码 `123456`，可通过 `.env` 中的 `APP_DEMO_PASSWORD` 修改。仅用于本机演示，不得用于共享或生产环境。

## Docker Compose演示

仓库根目录执行以下命令可启动MySQL、Spring Boot后端和Nginx前端：

```powershell
docker compose up --build
```

数据库数据保存在 `mysql-data` 卷中；需要重新执行初始化脚本时先执行 `docker compose down -v`。
默认开发密码仅用于本地演示，可通过根目录`.env`中的`DB_PASSWORD`和
`MYSQL_ROOT_PASSWORD`覆盖，禁止用于共享或生产环境。

## MySQL与上游整合

1. 后端唯一 Maven 工程位于 `backend/pom.xml`，唯一启动类为 `com.onlineexam.OnlineExamApplication`。
2. 业务包为 `auth`、`question`、`paper`、`answer`；公共响应、鉴权与配置位于 `common`、`config`。
3. 设置 `DB_URL`、`DB_USER`、`DB_PASSWORD` 并启用 MySQL 配置。不得把真实密码提交到仓库。
4. 上游报名资格与全组确认后的总表结构仍需按契约联调；Docker 资格适配器仅用于本地演示。

## 测试

```powershell
mvn -f backend/pom.xml clean verify
cd frontend
npm test
npm run build
```

测试范围和待验收项见 `docs/TEST-REPORT.md`；整合进度见 `docs/PROGRESS.md`。
