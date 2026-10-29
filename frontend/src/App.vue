<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from './api'
import { payload, remainingSeconds, makeSaveQueue } from './answer-state'

const token = ref(sessionStorage.getItem('task3-token') || '')
const examId = ref(1), tab = ref('answer'), context = ref(null), answers = ref({}), result = ref(null)
const busy = ref(false), dirty = ref(false), error = ref(''), savedAt = ref(''), seconds = ref(0)
const records = ref([]), page = ref(1), total = ref(0), statistics = ref(null)
const queue = makeSaveQueue()
let deadline = 0, offset = 0, timedOut = false
const editable = computed(() => context.value?.status === 'ANSWERING' && seconds.value > 0)
const timeText = computed(() => `${Math.floor(seconds.value / 3600)}:${String(Math.floor(seconds.value / 60) % 60).padStart(2, '0')}:${String(seconds.value % 60).padStart(2, '0')}`)
const answered = computed(() => Object.values(answers.value).filter(a => a.length).length)
const call = (method, url, data) => request(token.value, method, url, data)
function dateMs(value) { return Date.parse(value.replace(' ', 'T') + '+08:00') }
function apply(value) {
  context.value = value
  answers.value = Object.fromEntries(value.questions.map(q => [q.questionId, value.answers[q.questionId] || []]))
  deadline = dateMs(value.deadlineAt); offset = dateMs(value.serverTime) - Date.now()
  seconds.value = remainingSeconds(deadline, Date.now() + offset)
  dirty.value = false
}
async function run(job) {
  busy.value = true; error.value = ''
  try { await job() } catch (e) { error.value = e.message; ElMessage.error(e.message) }
  finally { busy.value = false }
}
function identityChanged() {
  sessionStorage.setItem('task3-token', token.value)
  context.value = null; result.value = null; statistics.value = null; records.value = []; timedOut = false
}
async function start() {
  await run(async () => {
    const value = await call('post', `/exams/${examId.value}/start`)
    apply(value); timedOut = false; result.value = null
    if (value.status === 'GRADED') result.value = await call('get', `/attempts/${value.attemptId}/result`)
  })
}
async function save() {
  if (!editable.value) return
  dirty.value = true
  await run(() => queue(async () => {
    const value = await call('put', `/attempts/${context.value.attemptId}/answers`, payload(answers.value, context.value.version))
    apply(value); savedAt.value = new Date().toLocaleTimeString('zh-CN')
  }))
}
function singleChanged(id, value) { answers.value[id] = [value]; save() }
async function submit(auto = false) {
  if (!context.value || context.value.status !== 'ANSWERING') return
  if (!auto) {
    try { await ElMessageBox.confirm(`已答 ${answered.value}/${context.value.questions.length} 题，确定交卷？未答题计0分。`, '提交答卷', { type: 'warning' }) }
    catch { return }
  }
  await run(() => queue(async () => {
    if (!auto && dirty.value) {
      const value = await call('put', `/attempts/${context.value.attemptId}/answers`, payload(answers.value, context.value.version))
      apply(value)
    }
    result.value = await call('post', `/attempts/${context.value.attemptId}/submit`)
    context.value.status = 'GRADED'; dirty.value = false
  }))
}
async function reload() { await run(async () => apply(await call('get', `/attempts/${context.value.attemptId}`))) }
async function loadScores() {
  await run(async () => {
    const value = await call('get', `/scores/my?page=${page.value}&size=10`)
    records.value = value.records; total.value = value.total
  })
}
async function loadStatistics() { await run(async () => statistics.value = await call('get', `/exams/${examId.value}/statistics`)) }
const timer = setInterval(() => {
  seconds.value = remainingSeconds(deadline, Date.now() + offset)
  if (context.value?.status === 'ANSWERING' && seconds.value === 0 && !timedOut && !busy.value) {
    timedOut = true; submit(true)
  }
}, 1000)
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <main>
    <header><div><small>ONLINE EXAM / TASK 03</small><h1>答题与成绩中心</h1><p>在线作答 · 自动评分 · 成绩统计</p></div><el-tag size="large">任务3独立联调页</el-tag></header>
    <el-alert title="演示需后端启用 demo 配置。演示数据存于内存，重启后清空；正式环境请接入团队登录与考试模块。" type="info" :closable="false" show-icon />
    <section class="toolbar">
      <label>访问凭证 <el-input v-model="token" type="password" show-password placeholder="输入团队登录返回的Token" :disabled="busy" @change="identityChanged" /></label>
      <label>演示身份 <el-select placeholder="选择后填入演示凭证" :disabled="busy" @change="value => { token = value; identityChanged() }"><el-option label="学生一" value="demo-student"/><el-option label="学生二" value="demo-student-2"/><el-option label="教师" value="demo-teacher"/></el-select></label>
      <label>考试ID <el-input-number v-model="examId" :min="1" :max="2147483647" :disabled="busy || editable"/></label>
    </section>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-tabs v-model="tab">
      <el-tab-pane label="在线答题" name="answer">
        <div class="actions"><el-button type="primary" :loading="busy" @click="start">进入 / 恢复考试</el-button><el-button v-if="context" :disabled="busy" @click="reload">重新载入服务端答案</el-button></div>
        <template v-if="context">
          <section class="summary"><h2>{{ context.examTitle }}</h2><span>已答 {{ answered }}/{{ context.questions.length }}</span><strong>{{ context.status === 'ANSWERING' ? timeText : '已交卷' }}</strong></section>
          <p role="status">{{ dirty ? '有未保存的答案，请重试保存；版本冲突时重新载入。' : savedAt ? `已保存于 ${savedAt}` : '已载入服务端答题进度' }}</p>
          <article v-for="(q,index) in context.questions" :key="q.questionId">
            <div class="question-title"><el-tag>{{ q.type === 'MULTIPLE_CHOICE' ? '多选题' : q.type === 'JUDGE' ? '判断题' : '单选题' }}</el-tag><span>{{ q.score }} 分</span></div>
            <h3>{{ index + 1 }}. {{ q.content }}</h3>
            <el-checkbox-group v-if="q.type === 'MULTIPLE_CHOICE'" v-model="answers[q.questionId]" :disabled="!editable || busy" @change="save"><el-checkbox v-for="o in q.options" :key="o.key" :value="o.key">{{ o.key }}. {{ o.label }}</el-checkbox></el-checkbox-group>
            <el-radio-group v-else :model-value="answers[q.questionId]?.[0]" :disabled="!editable || busy" @change="value => singleChanged(q.questionId,value)"><el-radio v-for="o in q.options" :key="o.key" :value="o.key">{{ o.label }}</el-radio></el-radio-group>
            <el-button v-if="editable" text :disabled="busy" @click="answers[q.questionId] = []; save()">清空本题</el-button>
          </article>
          <div class="actions"><el-button :disabled="!editable || busy" @click="save">保存全部答案</el-button><el-button type="primary" :disabled="context.status !== 'ANSWERING' || busy" @click="submit(false)">提交答卷</el-button></div>
          <el-result v-if="result" icon="success" title="评分完成" :sub-title="`${result.totalScore} / ${result.maxScore} 分 · ${result.submissionType === 'AUTO_SUBMITTED' ? '超时自动交卷' : '手动交卷'}`"/>
        </template>
        <el-empty v-else description="输入考试ID后，开始或恢复你的考试。演示考试ID为1。" />
      </el-tab-pane>
      <el-tab-pane label="我的成绩" name="scores">
        <div class="actions"><el-button :loading="busy" @click="loadScores">查询成绩</el-button></div>
        <el-table :data="records" empty-text="暂无成绩，请点击查询"><el-table-column prop="examTitle" label="考试"/><el-table-column prop="totalScore" label="得分"/><el-table-column prop="maxScore" label="满分"/><el-table-column prop="submittedAt" label="交卷时间" min-width="170"/></el-table>
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" :disabled="busy" @current-change="loadScores" />
      </el-tab-pane>
      <el-tab-pane label="教师统计" name="statistics">
        <div class="actions"><el-button :loading="busy" @click="loadStatistics">查询当前考试统计</el-button></div>
        <template v-if="statistics"><section class="metrics"><article v-for="(value,label) in {'参与人数': statistics.participantCount,'已评分人数':statistics.gradedCount,'平均分':statistics.averageScore ?? '—','最高分':statistics.maxScore ?? '—','最低分':statistics.minScore ?? '—','及格率':`${statistics.passRate}%`}" :key="label"><small>{{ label }}</small><h2>{{ value }}</h2></article></section>
          <h3>按卷面总分百分比划分</h3><div v-for="(count,range) in statistics.distribution" :key="range" class="bar"><span>{{ range }}%</span><el-progress :percentage="statistics.gradedCount ? count / statistics.gradedCount * 100 : 0" :format="() => `${count}人`" /></div></template>
        <el-empty v-else description="教师或管理员可查询有权限的考试统计" />
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<style>
body{margin:0;background:#f3f6fb;color:#20314c;font-family:system-ui,'Microsoft YaHei',sans-serif}main{max-width:1080px;margin:auto;padding:32px 24px 80px}header{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}h1{font-size:30px;margin:8px 0}header small{letter-spacing:2px;color:#3568a8}p{color:#64748b}.toolbar{display:flex;gap:20px;flex-wrap:wrap;padding:24px 0}.toolbar label{display:grid;gap:8px;flex:1;min-width:160px}.actions{display:flex;gap:12px;margin:20px 0;flex-wrap:wrap}article{background:white;border:1px solid #e2e8f0;border-radius:12px;padding:24px;margin:16px 0}.question-title{display:flex;justify-content:space-between;align-items:center}h3{line-height:1.7}.el-radio-group,.el-checkbox-group{display:flex;flex-direction:column;align-items:flex-start;gap:12px}.summary{display:flex;gap:24px;align-items:center;flex-wrap:wrap}.summary strong{font-size:24px;color:#2563eb}.metrics{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px}.metrics article{padding:20px}.bar{display:grid;grid-template-columns:90px 1fr;align-items:center;margin:20px 0}.el-pagination{margin-top:24px}@media(max-width:600px){main{padding:20px 14px}header{align-items:flex-start}header .el-tag{display:none}h1{font-size:25px}article{padding:18px}}
</style>
