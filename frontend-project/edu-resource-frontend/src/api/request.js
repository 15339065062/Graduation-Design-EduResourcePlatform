import axios from 'axios'
import { store } from '../store'

const api = axios.create({
  baseURL: 'http://localhost:8080/edu-resource-backend/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  config => {
    const token = store.state.token || localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      store.dispatch('logout')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
