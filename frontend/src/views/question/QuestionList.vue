<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="题型">
          <el-select v-model="query.questionType" placeholder="全部题型" clearable style="width: 160px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干关键字">
          <el-input
            v-model="query.keyword"
            placeholder="请输入题干关键字"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <span class="text-muted">
          共 {{ total }} 道题目 · {{ isAdmin ? '管理员可查看全部题目' : '教师仅可管理本人创建的题目' }}
        </span>
        <el-button type="primary" :icon="Plus" @click="goCreate">新增题目</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="题型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.questionType)" size="small">{{ row.typeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip />
        <el-table-column label="标准答案" width="130" align="center">
          <template #default="{ row }">
            <span class="answer-text">{{ (row.standardAnswer || []).join(' , ') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80" align="center" />
        <el-table-column prop="creatorName" label="创建人" width="100" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="goEdit(row.id)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无题目，点击右上角“新增题目”开始录入" />
        </template>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { listQuestions, deleteQuestion } from '@/api/question'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const typeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'JUDGE', label: '判断题' }
]

const query = reactive({ page: 1, size: 10, questionType: '', keyword: '' })
const records = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await listQuestions({
      page: query.page,
      size: query.size,
      questionType: query.questionType || undefined,
      keyword: query.keyword ? query.keyword.trim() : undefined
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
  query.questionType = ''
  query.keyword = ''
  query.page = 1
  loadData()
}

function goCreate() {
  router.push('/questions/create')
}

function goEdit(id) {
  router.push(`/questions/${id}/edit`)
}

async function handleDelete(row) {
  const summary = row.content.length > 20 ? `${row.content.slice(0, 20)}...` : row.content
  try {
    await ElMessageBox.confirm(`确定删除题目「${summary}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await deleteQuestion(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  loadData()
}

function typeTag(type) {
  const map = { SINGLE_CHOICE: 'primary', MULTIPLE_CHOICE: 'success', JUDGE: 'warning' }
  return map[type] || 'info'
}

onMounted(loadData)
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

.answer-text {
  font-family: Consolas, Monaco, monospace;
  color: #606266;
}
</style>
