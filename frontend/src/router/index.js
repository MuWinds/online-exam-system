import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'questions',
        name: 'questions',
        component: () => import('@/views/question/QuestionList.vue'),
        meta: { title: '题库管理', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'questions/create',
        name: 'question-create',
        component: () => import('@/views/question/QuestionForm.vue'),
        meta: { title: '新增题目', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'questions/:id/edit',
        name: 'question-edit',
        component: () => import('@/views/question/QuestionForm.vue'),
        meta: { title: '编辑题目', roles: ['ADMIN', 'TEACHER'] }
      },
      {
        path: 'exams',
        name: 'exams',
        component: () => import('@/views/exam/ExamCenter.vue'),
        meta: { title: '考试中心', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/auth/UserManage.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { public: true, title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    return true
  }
  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!userStore.user) {
    try {
      await userStore.loadCurrentUser()
    } catch (e) {
      userStore.token = ''
      localStorage.removeItem('token')
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.roles && !to.meta.roles.includes(userStore.user.roleCode)) {
    return { path: '/403' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 在线考试管理系统` : '在线考试管理系统'
})

export default router
