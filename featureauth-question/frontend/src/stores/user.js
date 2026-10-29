import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

/**
 * 登录状态：Token 持久化到 localStorage，用户信息保存在内存中。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null
  }),

  getters: {
    isLogin: (state) => Boolean(state.token),
    role: (state) => state.user?.roleCode || '',
    roleName: (state) => state.user?.roleName || ''
  },

  actions: {
    async login(form) {
      const res = await authApi.login(form)
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('token', this.token)
      return this.user
    },

    async loadCurrentUser() {
      const res = await authApi.getCurrentUser()
      this.user = res.data
      return this.user
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 退出接口失败不影响本地清理
      }
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
    }
  }
})
