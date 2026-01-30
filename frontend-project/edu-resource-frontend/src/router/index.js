import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: 'Home' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: 'Login' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: 'Register' }
  },
  {
    path: '/user-center',
    name: 'UserCenter',
    component: () => import('../views/UserCenter.vue'),
    meta: { requiresAuth: true, title: 'User Center' }
  },
  {
    path: '/resources',
    name: 'Resources',
    component: () => import('../views/Resources.vue'),
    meta: { title: 'Resources' }
  },
  {
    path: '/resources/:id',
    name: 'ResourceDetail',
    component: () => import('../views/ResourceDetail.vue'),
    meta: { title: 'Resource Detail' }
  },
  {
    path: '/upload',
    name: 'ResourceUpload',
    component: () => import('../views/ResourceUpload.vue'),
    meta: { requiresAuth: true, requiresTeacher: true, title: 'Upload Resource' }
  },
  {
    path: '/my-resources',
    name: 'MyResources',
    component: () => import('../views/MyResources.vue'),
    meta: { requiresAuth: true, title: 'My Resources' }
  },
  {
    path: '/my-collections',
    name: 'MyCollections',
    component: () => import('../views/MyCollections.vue'),
    meta: { requiresAuth: true, title: 'My Collections' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, title: 'System Admin' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '404 Not Found' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'Edu Resource Platform'
  
  if (to.meta.requiresAuth) {
    if (!store.getters.isLoggedIn) {
      next('/login')
      return
    }
    
    if (to.meta.requiresTeacher && store.getters.userRole !== 'teacher' && store.getters.userRole !== 'admin') {
      next('/resources')
      return
    }
    
    if (to.meta.requiresAdmin && store.getters.userRole !== 'admin') {
      next('/')
      return
    }
  }
  
  next()
})

export default router
