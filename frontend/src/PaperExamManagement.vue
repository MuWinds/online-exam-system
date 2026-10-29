<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from './exam-api'

const props = defineProps({ token: { type: String, default: '' }, role: { type: String, default: '' } })
const emit = defineEmits(['select-exam'])

const questions = ref([])
const papers = ref([])
const exams = ref([])
const available = ref([])
const activePaperId = ref(null)
const paperName = ref('')
const examName = ref('')
const selectedQuestionId = ref(null)
const questionScore = ref(null)
const selectedPaperId = ref(null)
const startAt = ref('')
const endAt = ref('')
const loading = ref(false)
const error = ref('')
const modeForRole = role => role === 'STUDENT' ? 'student' : 'teacher'
const mode = ref(modeForRole(props.role))
const selectedPaper = computed(() => papers.value.find(paper => paper.paperId === activePaperId.value))
const managerMode = computed(() => mode.value !== 'student')

function call(method, path, data) {
  return request(props.token, method, path, data)
}

function formatDate(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function defaultSchedule() {
  const start = new Date(Date.now() + 60 * 60 * 1000)
  start.setMinutes(0, 0, 0)
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
  startAt.value = formatDate(start)
  endAt.value = formatDate(end)
}

async function load() {
  if (!props.token) return
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'student') {
      available.value = await call('get', '/exams/available')
      return
    } else {
      try {
        const examRows = await call('get', '/managed/exams')
        mode.value = 'teacher'
        const [questionRows, paperRows] = await Promise.all([
          call('get', '/paper-questions'),
          call('get', '/papers')
        ])
        exams.value = examRows
        questions.value = questionRows
        papers.value = paperRows
        if (!papers.value.some(paper => paper.paperId === activePaperId.value)) {
          activePaperId.value = papers.value[0]?.paperId ?? null
        }
        if (!papers.value.some(paper => paper.paperId === selectedPaperId.value && paper.status === 'PUBLISHED')) {
          selectedPaperId.value = papers.value.find(paper => paper.status === 'PUBLISHED')?.paperId ?? null
        }
        return
      } catch (managerError) {
        try {
          available.value = await call('get', '/exams/available')
          mode.value = 'student'
          return
        } catch {
          throw managerError
        }
      }
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function perform(job, refresh = true) {
  loading.value = true
  error.value = ''
  try {
    await job()
    if (refresh) await load()
  } catch (e) {
    error.value = e.message
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function createPaper() {
  await perform(async () => {
    const paper = await call('post', '/papers', { paperName: paperName.value })
    activePaperId.value = paper.paperId
    paperName.value = ''
    ElMessage.success('试卷已创建')
  })
}

async function addQuestion() {
  if (!activePaperId.value || !selectedQuestionId.value) return
  await perform(async () => {
    const body = { questionId: selectedQuestionId.value }
    if (questionScore.value !== null && questionScore.value !== '') body.score = questionScore.value
    await call('post', `/papers/${activePaperId.value}/questions`, body)
    selectedQuestionId.value = null
    questionScore.value = null
  })
}

async function removeQuestion(questionId) {
  await perform(() => call('delete', `/papers/${activePaperId.value}/questions/${questionId}`))
}

async function publishPaper() {
  await perform(() => call('post', `/papers/${activePaperId.value}/publish`))
}

async function archivePaper(paper) {
  try {
    await ElMessageBox.confirm(`归档试卷“${paper.paperName}”？`, '归档试卷', { type: 'warning' })
  } catch { return }
  await perform(() => call('post', `/papers/${paper.paperId}/archive`))
}

async function createExam() {
  await perform(async () => {
    await call('post', '/managed/exams', {
      paperId: selectedPaperId.value,
      examName: examName.value,
      startAt: startAt.value,
      endAt: endAt.value
    })
    examName.value = ''
    ElMessage.success('考试已创建为未发布状态')
  })
}

async function publishExam(exam) {
  await perform(() => call('post', `/managed/exams/${exam.examId}/publish`))
}

async function unpublishExam(exam) {
  await perform(() => call('post', `/managed/exams/${exam.examId}/unpublish`))
}

watch(() => [props.token, props.role], () => {
  mode.value = modeForRole(props.role)
  load()
})
onMounted(() => {
  defaultSchedule()
  load()
})
</script>

<template>
  <section class="management">
    <div class="management-head">
      <div>
        <h2>{{ managerMode ? '试卷与考试管理' : '可参加的考试' }}</h2>
        <p>{{ managerMode ? '试卷组卷、发布与考试安排' : '当前账号可进入的已发布考试' }}</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />

    <template v-if="managerMode">
      <section class="management-section">
        <div class="section-title">
          <div><h3>试卷</h3><span>{{ papers.length }} 份</span></div>
          <el-tag type="info" effect="plain">草稿 → 已发布 → 归档</el-tag>
        </div>

        <el-form inline @submit.prevent="createPaper">
          <el-form-item label="新建试卷">
            <el-input v-model="paperName" maxlength="150" placeholder="试卷名称" />
          </el-form-item>
          <el-button type="primary" :disabled="!paperName.trim()" :loading="loading" @click="createPaper">创建</el-button>
        </el-form>

        <el-table :data="papers" highlight-current-row @row-click="paper => activePaperId = paper.paperId">
          <el-table-column prop="paperName" label="试卷名称" min-width="200" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column label="题数" width="90">
            <template #default="{ row }">{{ row.questions.length }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'ARCHIVED' ? 'info' : 'warning'">
                {{ row.status === 'DRAFT' ? '草稿' : row.status === 'PUBLISHED' ? '已发布' : '已归档' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click.stop="activePaperId = row.paperId">组卷</el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="success" :disabled="!row.questions.length" @click.stop="activePaperId = row.paperId; publishPaper()">发布</el-button>
              <el-button v-if="row.status !== 'ARCHIVED'" link type="danger" @click.stop="archivePaper(row)">归档</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedPaper" class="paper-editor">
          <div class="section-title">
            <div><h3>{{ selectedPaper.paperName }}</h3><span>总分 {{ selectedPaper.totalScore }}</span></div>
            <el-tag :type="selectedPaper.status === 'DRAFT' ? 'warning' : 'success'">
              {{ selectedPaper.status === 'DRAFT' ? '草稿可编辑' : '已发布锁定' }}
            </el-tag>
          </div>
          <el-form v-if="selectedPaper.status === 'DRAFT'" inline>
            <el-form-item label="题库题目">
              <el-select v-model="selectedQuestionId" placeholder="选择题目" filterable>
                <el-option v-for="question in questions" :key="question.questionId" :label="`${question.content}（${question.defaultScore}分）`" :value="question.questionId" />
              </el-select>
            </el-form-item>
            <el-form-item label="卷面分值">
              <el-input-number v-model="questionScore" :min="0.01" :max="9999.99" :precision="2" placeholder="默认分值" />
            </el-form-item>
            <el-button type="primary" :disabled="!selectedQuestionId" :loading="loading" @click="addQuestion">加入试卷</el-button>
          </el-form>
          <el-table :data="selectedPaper.questions" empty-text="从题库添加题目">
            <el-table-column prop="sortOrder" label="序号" width="75" />
            <el-table-column prop="content" label="题目" min-width="240" show-overflow-tooltip />
            <el-table-column prop="type" label="题型" width="150" />
            <el-table-column prop="score" label="分值" width="90" />
            <el-table-column v-if="selectedPaper.status === 'DRAFT'" label="" width="80">
              <template #default="{ row }">
                <el-button link type="danger" :disabled="loading" @click="removeQuestion(row.questionId)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="management-section">
        <div class="section-title"><div><h3>考试发布</h3><span>{{ exams.length }} 场</span></div></div>
        <el-form inline class="exam-form" @submit.prevent="createExam">
          <el-form-item label="考试名称"><el-input v-model="examName" maxlength="150" placeholder="考试名称" /></el-form-item>
          <el-form-item label="试卷">
            <el-select v-model="selectedPaperId" placeholder="选择已发布试卷">
              <el-option v-for="paper in papers.filter(item => item.status === 'PUBLISHED')" :key="paper.paperId" :label="`${paper.paperName} · ${paper.totalScore}分`" :value="paper.paperId" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="startAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="endAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" />
          </el-form-item>
          <el-button type="primary" :disabled="!examName.trim() || !selectedPaperId || !startAt || !endAt" :loading="loading" @click="createExam">创建考试</el-button>
        </el-form>
        <el-table :data="exams" empty-text="暂无考试">
          <el-table-column prop="examName" label="考试名称" min-width="180" />
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="startAt" label="开始时间" min-width="165" />
          <el-table-column prop="endAt" label="结束时间" min-width="165" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ONGOING' ? 'success' : row.status === 'FINISHED' ? 'info' : 'warning'">
                {{ row.published ? (row.status === 'NOT_STARTED' ? '待开始' : row.status === 'ONGOING' ? '进行中' : '已结束') : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.published" link type="success" :loading="loading" @click="publishExam(row)">发布</el-button>
              <el-button v-else-if="row.status === 'NOT_STARTED'" link type="warning" :loading="loading" @click="unpublishExam(row)">撤回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <el-table v-else :data="available" v-loading="loading" empty-text="暂无可参加的考试">
      <el-table-column prop="examName" label="考试名称" min-width="220" />
      <el-table-column prop="totalScore" label="满分" width="100" />
      <el-table-column prop="startAt" label="开始时间" min-width="170" />
      <el-table-column prop="endAt" label="结束时间" min-width="170" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ONGOING' ? 'success' : 'warning'">{{ row.status === 'ONGOING' ? '进行中' : '待开始' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" :disabled="row.status !== 'ONGOING'" @click="emit('select-exam', row.examId)">进入考试</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.management{padding:8px 0 32px;color:#243347}
.management-head,.section-title{display:flex;align-items:center;justify-content:space-between;gap:16px}
.management-head{margin:8px 0 20px}
.management-head h2,.section-title h3{margin:0;font-size:19px;font-weight:650}
.management-head p{margin:5px 0 0;color:#718096;font-size:14px}
.management-section{padding:22px 0;border-top:1px solid #e3e8ee}
.section-title{margin-bottom:16px}
.section-title>div{display:flex;align-items:baseline;gap:12px}
.section-title span{color:#78879a;font-size:13px}
.paper-editor{margin-top:22px;padding-top:20px;border-top:1px solid #e3e8ee}
.exam-form{align-items:flex-start}
@media(max-width:720px){.management-head{align-items:flex-start}.section-title{align-items:flex-start;flex-direction:column}.section-title>div{flex-wrap:wrap}.exam-form{display:flex;flex-direction:column}.exam-form .el-form-item{margin-right:0}}
</style>
