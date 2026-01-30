import axios from 'axios'
import store from '../store'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  (config) => {
    const token = store.state.token || localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        store.dispatch('logout')
        window.location.href = '/login'
      }
      return Promise.reject({
        success: false,
        message: error.response.data?.message || 'Request failed',
        code: error.response.status
      })
    } else if (error.request) {
      return Promise.reject({
        success: false,
        message: 'Network error. Please check your connection.'
      })
    } else {
      return Promise.reject({
        success: false,
        message: 'An unexpected error occurred.'
      })
    }
  }
)

export default apiClient
