import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import './styles/global.less'
import { userApi } from '@/api/user-api'

const app = createApp(App)

app.use(router)
app.use(store)

// Initialize user from storage
store.dispatch('loadUserFromStorage')

// Verify token validity
if (store.state.token) {
  userApi.getUserInfo()
    .then(response => {
      if (response.success) {
        store.commit('UPDATE_USER_INFO', response.data)
        console.log('User session restored')
      } else {
        console.warn('Session restoration failed, clearing storage')
        store.dispatch('logout')
      }
    })
    .catch(err => {
      console.error('Failed to restore session:', err)
      // If network error, keep session optimistically
      // If 401/403, interceptor will handle it
    })
}

app.mount('#app')
