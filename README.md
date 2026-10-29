# 在线考试管理系统

软件工程课程设计。当前分支 `feature/answer-score` 包含任务3的独立开发与联调版本。

已实现在线答题、保存和恢复、手动及服务端超时交卷、三类客观题自动评分、个人成绩分页和教师考试统计。任务1的真实登录/题库和任务2的真实试卷/考试发布尚未接入；全系统验收需等待联调。

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
选择“学生一”，考试ID填1，进入考试。演示有单选、多选和判断三题，满分100。切换“教师”后可在教师统计页查询考试1。
演示题目的标准答案只在后端；演示身份仅是固定凭证，不属于真实登录实现。

本机Java21曾因默认临时目录触发 `Unable to establish loopback connection`。若遇到同样错误，可在仓库根目录用以下方式重试（target需先构建生成）：

```powershell
java "-Djdk.net.unixdomain.tmpdir=$((Resolve-Path backend/target).Path)" -jar backend/target/answer-score-0.1.0.jar --spring.profiles.active=demo
```

demo使用内存H2数据库，重启清空。不要把demo配置用于共享环境或正式部署。默认没有启用demo，没有真实适配器时启动失败是预期行为。

## MySQL与上游整合

1. 任务2先审阅 `sql/task3.sql` 和 `docs/DATABASE-CONTRACT.md`，合并详细字段到总SQL，再在MySQL8环境导入。脚本不会迁移已有表。
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
