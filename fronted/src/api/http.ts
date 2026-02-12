import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('cw_token')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const data = response.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        data.code = 200
      }
    }
    return response
  },
  error => {
    return Promise.reject(error)
  }
)

