<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="账号或姓名"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" placeholder="全部角色" clearable style="width: 150px">
            <el-option v-for="role in roles" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <span class="text-muted">共 {{ total }} 个用户</span>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTag(row.roleCode)" size="small">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" :icon="Key" @click="openResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="editDialog.visible" :title="editDialog.title" width="460px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="editForm.username" :disabled="!editDialog.isCreate" placeholder="4~20位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="editForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editDialog.isCreate" label="初始密码" prop="password">
          <el-input v-model="editForm.password" placeholder="留空则使用默认密码 123456" show-password />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetDialog.visible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="80px">
        <el-form-item label="账号">
          <el-input :model-value="resetDialog.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" placeholder="6~20位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="resetDialog.submitting" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Edit, Key } from '@element-plus/icons-vue'
import { createUser, listRoles, listUsers, resetUserPassword, updateUser } from '@/api/user'

const roles = ref([])
const records = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ page: 1, size: 10, keyword: '', roleCode: '' })

const editFormRef = ref()
const editDialog = reactive({ visible: false, title: '', isCreate: true, submitting: false })
const editForm = reactive({ id: null, username: '', realName: '', roleCode: 'TEACHER', password: '', status: 1 })

const editRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '账号必须为4~20位字母、数字或下划线', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ min: 6, max: 20, message: '密码长度必须为6~20位', trigger: 'blur' }]
}

const resetFormRef = ref()
const resetDialog = reactive({ visible: false, username: '', id: null, submitting: false })
const resetForm = reactive({ newPassword: '' })
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须为6~20位', trigger: 'blur' }
  ]
}

async function loadRoles() {
  const res = await listRoles()
  roles.value = res.data
}

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword ? query.keyword.trim() : undefined,
      roleCode: query.roleCode || undefined
    })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.roleCode = ''
  query.page = 1
  loadData()
}

function openCreate() {
  editDialog.isCreate = true
  editDialog.title = '新增用户'
  editDialog.visible = true
  Object.assign(editForm, { id: null, username: '', realName: '', roleCode: 'TEACHER', password: '', status: 1 })
}

function openEdit(row) {
  editDialog.isCreate = false
  editDialog.title = '编辑用户'
  editDialog.visible = true
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    roleCode: row.roleCode,
    password: '',
    status: row.status
  })
}

async function submitEdit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  editDialog.submitting = true
  try {
    if (editDialog.isCreate) {
      await createUser({
        username: editForm.username.trim(),
        realName: editForm.realName.trim(),
        roleCode: editForm.roleCode,
        password: editForm.password || null,
        status: editForm.status
      })
      ElMessage.success('新增成功')
    } else {
      await updateUser(editForm.id, {
        realName: editForm.realName.trim(),
        roleCode: editForm.roleCode,
        status: editForm.status
      })
      ElMessage.success('保存成功')
    }
    editDialog.visible = false
    loadData()
  } finally {
    editDialog.submitting = false
  }
}

function openResetPassword(row) {
  resetDialog.visible = true
  resetDialog.id = row.id
  resetDialog.username = row.username
  resetForm.newPassword = ''
}

async function submitReset() {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  resetDialog.submitting = true
  try {
    await resetUserPassword(resetDialog.id, { newPassword: resetForm.newPassword })
    ElMessage.success('密码已重置')
    resetDialog.visible = false
  } finally {
    resetDialog.submitting = false
  }
}

async function handleStatusChange(row) {
  try {
    await updateUser(row.id, {
      realName: row.realName,
      roleCode: row.roleCode,
      status: row.status
    })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch (e) {
    // 失败时回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
}

function roleTag(code) {
  const map = { ADMIN: 'danger', TEACHER: 'success', STUDENT: 'info' }
  return map[code] || 'info'
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.filter-form {
  margin-bottom: 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
