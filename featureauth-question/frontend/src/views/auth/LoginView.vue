<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <h1>在线考试管理系统</h1>
        <p class="subtitle">软件工程课程设计 · 题目4</p>
        <ul class="feature-list">
          <li>管理员：管理系统用户与角色权限</li>
          <li>教师：维护题库、组卷与阅卷</li>
          <li>学生：参加考试、查询个人成绩</li>
        </ul>
      </div>

      <div class="login-form">
        <h2>账号登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入账号"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="login-error"
        />

        <div class="demo-accounts">
          <div class="demo-title">演示账号（密码均为 123456，点击自动填充）</div>
          <div class="demo-tags">
            <el-tag class="demo-tag" @click="fillAccount('admin')">admin / 管理员</el-tag>
            <el-tag class="demo-tag" type="success" @click="fillAccount('teacher')">teacher / 教师</el-tag>
            <el-tag class="demo-tag" type="warning" @click="fillAccount('student')">student / 学生</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const errorMessage = ref('')
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fillAccount(username) {
  form.username = username
  form.password = '123456'
  errorMessage.value = ''
}

async function handleLogin() {
  if (loading.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.push(redirect)
  } catch (e) {
    errorMessage.value = e.response?.data?.message || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3f6d 0%, #2f6fa8 100%);
}

.login-card {
  display: flex;
  width: 860px;
  min-height: 440px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.25);
}

.login-brand {
  width: 46%;
  padding: 48px 36px;
  color: #fff;
  background: linear-gradient(160deg, #24476f, #3a7fb8);
}

.login-brand h1 {
  margin: 0 0 12px;
  font-size: 26px;
  line-height: 1.4;
}

.subtitle {
  margin: 0 0 32px;
  opacity: 0.85;
  font-size: 14px;
}

.feature-list {
  padding-left: 18px;
  margin: 0;
  line-height: 2.2;
  font-size: 14px;
  opacity: 0.95;
}

.login-form {
  flex: 1;
  padding: 48px 44px;
  box-sizing: border-box;
}

.login-form h2 {
  margin: 0 0 24px;
  font-size: 20px;
  color: #303133;
}

.login-button {
  width: 100%;
  margin-top: 4px;
}

.login-error {
  margin-top: 16px;
}

.demo-accounts {
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px dashed #e4e7ed;
}

.demo-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.demo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}
</style>
