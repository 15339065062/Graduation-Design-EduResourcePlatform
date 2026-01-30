import request from '@/utils/request'
import store from '@/store'

export const userApi = {
  login(username, password) {
    return request.post('/user/login', { username, password })
  },

  register(data) {
    return request.post('/user/register', data)
  },

  logout() {
    return request.post('/user/logout')
  },

  refreshToken() {
    return request.post('/user/refresh')
  },

  getUserInfo() {
    return request.get('/user/profile')
  },

  updateProfile(data) {
    return request.put('/user/profile', data)
  },

  changePassword(oldPassword, newPassword) {
    return request.put('/user/password', { oldPassword, newPassword })
  },

  uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    return request.post('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  submitRoleRequest(data) {
    return request.post('/user/role-request', data)
  },

  getPublicProfile(userId) {
    return request.get(`/user/${userId}`)
  }
}
