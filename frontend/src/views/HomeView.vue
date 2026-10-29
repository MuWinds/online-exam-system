<template>
  <div class="page-container">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <div>
          <h2>你好，{{ userStore.user?.realName || userStore.user?.username }}</h2>
          <p class="text-muted">
            当前角色：{{ userStore.roleName }}（{{ userStore.role }}） ·
            登录账号：{{ userStore.user?.username }}
          </p>
        </div>
        <el-tag type="success" effect="plain">任务1：登录权限与题库管理</el-tag>
      </div>
    </el-card>

    <el-row :gutter="16" class="module-row">
      <el-col v-for="item in modules" :key="item.path" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="module-card" @click="router.push(item.path)">
          <div class="module-icon">
            <el-icon :size="26"><component :is="item.icon" /></el-icon>
          </div>
          <div class="module-body">
            <div class="module-title">{{ item.title }}</div>
            <div class="text-muted">{{ item.desc }}</div>
          </div>
          <el-icon class="module-arrow"><ArrowRight /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="plan-card">
      <template #header>
        <div class="plan-header">
          <span>模块开发进度（按小组任务分工）</span>
          <el-tag size="small" type="info">更新至任务1交付</el-tag>
        </div>
      </template>
      <el-table :data="plan" size="small">
        <el-table-column prop="module" label="模块" width="180" />
        <el-table-column prop="owner" label="负责任务" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.tag" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, UserFilled, EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const allModules = [
  { path: '/questions', title: '题库管理', desc: '题目的新增、修改、删除与检索', icon: Collection, roles: ['ADMIN', 'TEACHER'] },
  { path: '/users', title: '用户管理', desc: '账号、角色与启停用管理', icon: UserFilled, roles: ['ADMIN'] },
  { path: '/profile', title: '个人中心', desc: '查看个人信息与修改密码', icon: EditPen, roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
]

const modules = computed(() => allModules.filter((item) => item.roles.includes(userStore.role)))

const plan = [
  { module: '登录与权限', owner: '任务1', status: '已完成', tag: 'success', remark: 'JWT 登录、角色鉴权、用户管理' },
  { module: '题库管理', owner: '任务1', status: '已完成', tag: 'success', remark: '单选、多选、判断三种题型' },
  { module: '试卷管理', owner: '任务2', status: '开发中', tag: 'warning', remark: '组卷、试卷预览，接口由任务2提供' },
  { module: '考试发布', owner: '任务2', status: '开发中', tag: 'warning', remark: '考试信息与发布规则' },
  { module: '在线答题', owner: '任务3', status: '待联调', tag: 'info', remark: '依赖任务2的试卷与考试接口' },
  { module: '自动评分与成绩', owner: '任务3', status: '待联调', tag: 'info', remark: '依赖任务1的标准答案字段' }
]
</script>

<style scoped>
.welcome-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.welcome {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #303133;
}

.module-row {
  margin-bottom: 8px;
}

.module-card {
  margin-bottom: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.module-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.module-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.module-body {
  flex: 1;
}

.module-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.module-arrow {
  color: #c0c4cc;
}

.plan-card {
  border-radius: 8px;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
