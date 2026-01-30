import axios from 'axios'
import store from '@/store'
import { isTokenExpiringSoon } from './jwt'

const API_BASE_URL = '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let isRefreshing = false
let requests = []

request.interceptors.request.use(
  async config => {
    let token = store.state.token
    console.log('Request URL:', config.url)

    if (config.data instanceof FormData) {
      if (config.headers) {
        delete config.headers['Content-Type']
        delete config.headers['content-type']
      }
    }
    
    // Skip token check for refresh request to avoid infinite loop
    if (config.url.includes('/user/refresh')) {
      if (token) {
        config.headers['Authorization'] = `Bearer ${token}`
      }
      return config
    }

    if (token) {
      if (isTokenExpiringSoon(token)) {
        if (!isRefreshing) {
          isRefreshing = true
          try {
            console.log('Token expiring soon, refreshing...')
            const res = await request.post('/user/refresh')
            if (res.success && res.data) {
              const newToken = res.data
              console.log('Token refreshed successfully')
              store.commit('SET_TOKEN', newToken)
              localStorage.setItem('token', newToken)
              token = newToken
              
              // Process queue
              requests.forEach(cb => cb(newToken))
              requests = []
            }
          } catch (e) {
            console.error('Refresh token failed', e)
            store.dispatch('logout')
            window.location.href = '/login'
            return Promise.reject(e)
          } finally {
            isRefreshing = false
          }
        } else {
          // Add to queue
          return new Promise(resolve => {
            requests.push((newToken) => {
              config.headers['Authorization'] = `Bearer ${newToken}`
              resolve(config)
            })
          })
        }
      }
      
      config.headers['Authorization'] = `Bearer ${token}`
      console.log('Added Authorization header')
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      console.log('Response error status:', error.response.status)
      console.log('Response error data:', error.response.data)
      
      switch (error.response.status) {
        case 401:
          console.log('Token is invalid or expired, logging out user')
          // Check if it's a login attempt failure, don't clear storage then
          if (!error.config.url.includes('/user/login')) {
              // Avoid alert loops if multiple requests fail
              if (!document.querySelector('.auth-alert')) {
                 // alert('登录已过期，请重新登录')
                 // document.body.classList.add('auth-alert-shown')
                 console.warn('Session expired')
              }
              store.dispatch('logout')
              // Only redirect if not already on login page
              if (window.location.pathname !== '/login') {
                  window.location.href = '/login'
              }
          }
          break
        case 403:
          console.error('Access denied')
          // Check for account disabled message
          if (error.response.data && error.response.data.message === 'Account is disabled') {
             alert('您的账号已被禁用，将自动退出')
             store.dispatch('logout')
             window.location.href = '/login'
          } else {
             alert('没有权限执行此操作')
          }
          break
        case 404:
          console.error('Resource not found')
          alert('请求的资源不存在')
          break
        case 500:
          console.error('Server error')
          alert('服务器错误，请稍后重试')
          break
        default:
          console.error('Request failed:', error.message)
          alert(`请求失败: ${error.response.data?.message || error.response.statusText}`)
      }
    } else {
      console.error('Network error:', error.message)
      alert('网络错误，请检查您的网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
