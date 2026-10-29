<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>个人信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="登录账号">{{ user?.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ user?.realName }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag size="small">{{ user?.roleName }}（{{ user?.roleCode }}）</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="user?.status === 1 ? 'success' : 'info'" size="small">
                {{ user?.status === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ user?.createdAt }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password placeholder="6~20位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const user = userStore.user

const formRef = ref()
const submitting = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须为6~20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } finally {
    submitting.value = false
  }
}
</script>
