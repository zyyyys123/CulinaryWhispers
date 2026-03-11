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
      if (data.code === 401) {
        localStorage.removeItem('cw_token')
        window.dispatchEvent(new Event('cw:auth:clear'))
        const current = window.location.pathname + window.location.search + window.location.hash
        if (!current.startsWith('/login')) {
          window.location.href = `/login?redirect=${encodeURIComponent(current)}`
        }
        return Promise.reject(new Error('Unauthorized'))
      }
    }
    return response
  },
  error => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('cw_token')
      window.dispatchEvent(new Event('cw:auth:clear'))
      const current = window.location.pathname + window.location.search + window.location.hash
      if (!current.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(current)}`
      }
    }
    return Promise.reject(error)
  }
)
