import request from './request'

/** 登录 */
export const login = (data) => request.post('/auth/login', data)

/** 获取当前登录用户 */
export const getCurrentUser = () => request.get('/auth/current-user')

/** 退出登录 */
export const logout = () => request.post('/auth/logout')

/** 修改本人密码 */
export const changePassword = (data) => request.put('/auth/password', data)
