import axios from 'axios'
const http = axios.create({ baseURL: '/api', timeout: 15000 })
export async function request(token, method, url, data) {
  try {
    const response = await http.request({ method, url, data, headers: { Authorization: `Bearer ${token}` } })
    if (response.data.code !== 200) throw new Error(response.data.message || '请求失败')
    return response.data.data
  } catch (e) { throw new Error(e.response?.data?.message || e.message || '网络连接失败') }
}
