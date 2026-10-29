import request from './request'

/** 题目分页查询 */
export const listQuestions = (params) => request.get('/questions', { params })

/** 题目详情 */
export const getQuestion = (id) => request.get(`/questions/${id}`)

/** 新增题目 */
export const createQuestion = (data) => request.post('/questions', data)

/** 修改题目 */
export const updateQuestion = (id, data) => request.put(`/questions/${id}`, data)

/** 删除题目 */
export const deleteQuestion = (id) => request.delete(`/questions/${id}`)
