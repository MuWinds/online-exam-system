import request from './request'

/** 用户分页查询 */
export const listUsers = (params) => request.get('/users', { params })

/** 新增用户 */
export const createUser = (data) => request.post('/users', data)

/** 修改用户 */
export const updateUser = (id, data) => request.put(`/users/${id}`, data)

/** 重置密码 */
export const resetUserPassword = (id, data) => request.put(`/users/${id}/password`, data)

/** 角色列表 */
export const listRoles = () => request.get('/roles')
