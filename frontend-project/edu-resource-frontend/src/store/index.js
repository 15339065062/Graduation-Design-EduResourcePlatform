import { createStore } from 'vuex'

const store = createStore({
  state: {
    user: {
      isLoggedIn: false,
      id: null,
      username: '',
      nickname: '',
      phone: '',
      role: '',
      avatar: ''
    },
    token: localStorage.getItem('token') || ''
  },
  
  getters: {
    isLoggedIn: state => state.user.isLoggedIn && !!state.token,
    currentUser: state => state.user,
    userRole: state => state.user.role,
    isAdmin: state => state.user.role === 'admin',
    isTeacher: state => state.user.role === 'teacher',
    isStudent: state => state.user.role === 'student',
    canUpload: state => state.user.role === 'teacher' || state.user.role === 'admin'
  },
  
  mutations: {
    SET_USER(state, user) {
      state.user = {
        isLoggedIn: true,
        ...user
      }
    },
    
    SET_TOKEN(state, token) {
      state.token = token
    },
    
    CLEAR_USER(state) {
      state.user = {
        isLoggedIn: false,
        id: null,
        username: '',
        nickname: '',
        phone: '',
        role: '',
        avatar: ''
      }
      state.token = ''
    },
    
    UPDATE_USER_INFO(state, userInfo) {
      state.user = {
        ...state.user,
        ...userInfo
      }
    }
  },
  
  actions: {
    login({ commit }, { user, token }) {
      commit('SET_USER', user)
      commit('SET_TOKEN', token)
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    
    logout({ commit }) {
      commit('CLEAR_USER')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    
    loadUserFromStorage({ commit }) {
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('user')
      
      console.log('Loading user from storage:', { token, userStr })
      
      if (token && userStr) {
        try {
          const user = JSON.parse(userStr)
          commit('SET_TOKEN', token)
          commit('SET_USER', user)
          console.log('User loaded from storage successfully')
        } catch (error) {
          console.error('Failed to parse user from storage:', error)
          // Clear invalid data
          localStorage.removeItem('token')
          localStorage.removeItem('user')
        }
      } else {
        console.log('No valid token or user found in storage')
      }
    },
    
    updateUserInfo({ commit }, userInfo) {
      commit('UPDATE_USER_INFO', userInfo)
      const currentUser = JSON.parse(localStorage.getItem('user'))
      localStorage.setItem('user', JSON.stringify({ ...currentUser, ...userInfo }))
    }
  }
})

export default store
