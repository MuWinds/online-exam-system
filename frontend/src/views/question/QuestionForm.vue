<template>
  <div class="page-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑题目' : '新增题目' }}</span>
          <el-button link :icon="Back" @click="router.push('/questions')">返回列表</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="question-form">
        <el-form-item label="题型" prop="type">
          <el-radio-group v-model="form.type" @change="handleTypeChange">
            <el-radio-button value="SINGLE_CHOICE">单选题</el-radio-button>
            <el-radio-button value="MULTIPLE_CHOICE">多选题</el-radio-button>
            <el-radio-button value="JUDGE">判断题</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题干" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="请输入题干，例如：下列选项中，不属于面向对象三大特征的是（  ）。"
          />
        </el-form-item>

        <el-form-item label="分值" prop="score">
          <el-input-number v-model="form.score" :min="0.5" :max="100" :step="0.5" :precision="2" />
          <span class="form-tip">分</span>
        </el-form-item>

        <el-form-item v-if="isJudge" label="正确答案">
          <el-radio-group v-model="form.judgeAnswer">
            <el-radio value="TRUE">正确</el-radio>
            <el-radio value="FALSE">错误</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-else label="选项与答案">
          <div class="options">
            <el-checkbox-group v-model="form.answers" class="option-group" @change="handleAnswerChange">
              <div v-for="(option, index) in form.options" :key="option.optionKey" class="option-row">
                <el-tag class="option-key">{{ option.optionKey }}</el-tag>
                <el-input
                  v-model="option.content"
                  placeholder="请输入选项内容"
                  maxlength="200"
                  class="option-input"
                />
                <el-checkbox :value="option.optionKey" class="correct-check">正确答案</el-checkbox>
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  :disabled="form.options.length <= 2"
                  @click="removeOption(index)"
                />
              </div>
            </el-checkbox-group>
            <div class="option-actions">
              <el-button type="primary" plain :icon="Plus" :disabled="form.options.length >= 6" @click="addOption">
                添加选项
              </el-button>
              <span class="text-muted">
                {{ isMultiple ? '多选题至少勾选 2 个正确答案' : '单选题只能勾选 1 个正确答案' }}
              </span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="解析">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="选填，用于展示答案解析"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="router.push('/questions')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Delete, Plus } from '@element-plus/icons-vue'
import { createQuestion, getQuestion, updateQuestion } from '@/api/question'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const isEdit = ref(false)

const form = reactive({
  type: 'SINGLE_CHOICE',
  content: '',
  score: 2,
  analysis: '',
  judgeAnswer: 'TRUE',
  answers: [],
  options: []
})

const rules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  score: [{ required: true, message: '请输入分值', trigger: 'change' }]
}

const isJudge = computed(() => form.type === 'JUDGE')
const isMultiple = computed(() => form.type === 'MULTIPLE_CHOICE')

function defaultOptions() {
  return [
    { optionKey: 'A', content: '' },
    { optionKey: 'B', content: '' },
    { optionKey: 'C', content: '' },
    { optionKey: 'D', content: '' }
  ]
}

function resetForm() {
  form.type = 'SINGLE_CHOICE'
  form.content = ''
  form.score = 2
  form.analysis = ''
  form.judgeAnswer = 'TRUE'
  form.answers = []
  form.options = defaultOptions()
  formRef.value?.clearValidate()
}

function handleTypeChange(type) {
  if (type === 'JUDGE') {
    form.answers = []
    return
  }
  if (!form.options || form.options.length < 2) {
    form.options = defaultOptions()
  }
  form.answers = []
}

function handleAnswerChange(values) {
  if (form.type === 'SINGLE_CHOICE' && values.length > 1) {
    form.answers = [values[values.length - 1]]
  }
}

function nextOptionKey() {
  const used = new Set(form.options.map((item) => item.optionKey))
  for (const key of ['A', 'B', 'C', 'D', 'E', 'F']) {
    if (!used.has(key)) {
      return key
    }
  }
  return 'F'
}

function addOption() {
  if (form.options.length >= 6) {
    return
  }
  form.options.push({ optionKey: nextOptionKey(), content: '' })
}

function removeOption(index) {
  if (form.options.length <= 2) {
    return
  }
  const removedKey = form.options[index].optionKey
  form.options.splice(index, 1)
  form.answers = form.answers.filter((key) => key !== removedKey)
}

async function init() {
  const id = route.params.id
  isEdit.value = Boolean(id)
  resetForm()
  if (!id) {
    return
  }
  loading.value = true
  try {
    const res = await getQuestion(id)
    const data = res.data
    form.type = data.questionType
    form.content = data.content
    form.score = Number(data.score)
    form.analysis = data.analysis || ''
    if (data.questionType === 'JUDGE') {
      form.judgeAnswer = (data.standardAnswer && data.standardAnswer[0]) || 'TRUE'
    } else {
      form.options = (data.options || []).map((item) => ({
        optionKey: item.optionKey,
        content: item.optionContent
      }))
      form.answers = [...(data.standardAnswer || [])]
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  let payload
  if (isJudge.value) {
    payload = {
      questionType: form.type,
      content: form.content.trim(),
      score: form.score,
      analysis: form.analysis ? form.analysis.trim() : null,
      answer: [form.judgeAnswer],
      options: []
    }
  } else {
    if (form.options.length < 2) {
      ElMessage.warning('选择题至少需要 2 个选项')
      return
    }
    if (form.options.some((item) => !item.content.trim())) {
      ElMessage.warning('选项内容不能为空')
      return
    }
    if (form.answers.length === 0) {
      ElMessage.warning('请勾选正确答案')
      return
    }
    if (isMultiple.value && form.answers.length < 2) {
      ElMessage.warning('多选题至少需要两个正确答案')
      return
    }
    if (!isMultiple.value && form.answers.length !== 1) {
      ElMessage.warning('单选题必须且只能有一个正确答案')
      return
    }
    payload = {
      questionType: form.type,
      content: form.content.trim(),
      score: form.score,
      analysis: form.analysis ? form.analysis.trim() : null,
      answer: [...form.answers].sort(),
      options: form.options.map((item) => ({
        optionKey: item.optionKey,
        content: item.content.trim()
      }))
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateQuestion(route.params.id, payload)
    } else {
      await createQuestion(payload)
    }
    ElMessage.success('保存成功')
    router.push('/questions')
  } finally {
    submitting.value = false
  }
}

watch(() => route.params.id, () => init(), { immediate: true })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-form {
  max-width: 860px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
}

.options {
  width: 100%;
}

.option-group {
  display: block;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-key {
  width: 32px;
  height: 28px;
  justify-content: center;
  flex-shrink: 0;
}

.option-input {
  flex: 1;
}

.correct-check {
  width: 92px;
  flex-shrink: 0;
}

.option-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
