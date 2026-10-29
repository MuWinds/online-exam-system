# 在线考试管理系统

软件工程课程设计。当前分支 `feature/paper-exam` 包含任务2试卷/考试管理和任务3答题评分的独立演示版本。

已实现试卷创建、题目组卷、试卷发布、考试配置与发布、学生可参加考试查询，以及在线答题、保存和恢复、手动及服务端超时交卷、三类客观题自动评分、个人成绩分页和教师考试统计。demo使用固定身份和H2数据，正式联调仍需接入任务1真实登录/题库和确认后的总SQL。

## 环境

- Java 17及以上、Maven 3.9；编译目标Java17。
- 前端团队约定Node20，当前验证环境Node24。
- Spring Boot3.3.5、MyBatis-Plus3.5.5；前端Vue3、Element Plus、Axios、Vite。具体补丁版本属于本次独立模块选取，需与公共工程对齐。

## 独立演示

在仓库根目录启动后端：

```powershell
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=demo"
```

或先打包再启动：

```powershell
mvn -f backend/pom.xml clean package
java -jar backend/target/answer-score-0.1.0.jar --spring.profiles.active=demo
```

另一个终端启动前端：

```powershell
cd frontend
npm ci
npm run dev
```

打开Vite输出的本地地址，默认 http://127.0.0.1:5173。前端将/api代理到后端8080。
选择“教师”后进入“试卷与考试”页，可以创建草稿、从题库组卷、发布试卷、配置时间并发布考试；选择“学生一”后可在同页查看已发布考试并进入答题。演示考试ID为1，含单选、多选和判断三题，满分100；任务3统计页仍可查询考试1。
演示题目的标准答案只在后端；演示身份仅是固定凭证，不属于真实登录实现。

本机Java21曾因默认临时目录触发 `Unable to establish loopback connection`。若遇到同样错误，可在仓库根目录用以下方式重试（target需先构建生成）：

```powershell
java "-Djdk.net.unixdomain.tmpdir=$((Resolve-Path backend/target).Path)" -jar backend/target/answer-score-0.1.0.jar --spring.profiles.active=demo
```

demo使用内存H2数据库，重启清空。不要把demo配置用于共享环境或正式部署。默认没有启用demo，没有真实适配器时启动失败是预期行为。

## Docker Compose演示

仓库根目录执行以下命令可启动MySQL、Spring Boot后端和Nginx前端：

```powershell
docker compose up --build
```

打开 http://127.0.0.1:8088。Compose使用MySQL初始化脚本和固定演示身份，学生凭证为
`Bearer demo-student`，教师凭证为 `Bearer demo-teacher`。数据库数据保存在
`mysql-data`卷中；需要重新执行初始化脚本时先执行 `docker compose down -v`。
默认开发密码仅用于本地演示，可通过根目录`.env`中的`DB_PASSWORD`和
`MYSQL_ROOT_PASSWORD`覆盖，禁止用于共享或生产环境。

## MySQL与上游整合

1. 任务2先审阅 `sql/task2.sql`、`sql/task3.sql` 和 `docs/DATABASE-CONTRACT.md`，合并详细字段到总SQL，再在MySQL8环境导入。脚本不会迁移已有表。
2. 任务1提供 `IdentityGateway` 实现；任务1、2联合提供 `ExamGateway` 实现。包名接口位于backend/src/main/java/com/onlineexam/task3。
3. 设置DB_URL、DB_USER、DB_PASSWORD并启用mysql配置。不得把真实密码提交到仓库。
4. 公共工程整合时使用团队唯一的启动类和公共响应/异常处理，导入任务3组件与Mapper；统一Spring版本、前端入口和鉴权。
5. 完成MySQL并发/超时/回滚验证及全组业务主流程后再标记整体测试完成。

## 测试

```powershell
mvn -f backend/pom.xml clean verify
cd frontend
npm test
npm run build
```

测试范围和待验收项见 `docs/TEST-REPORT.md`；开发记录见 `docs/TASK3-IMPLEMENTATION.md`。
