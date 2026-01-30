<template>
  <div class="navbar">
    <div class="container">
      <div class="navbar-brand">
        <router-link to="/" class="logo">
          <i class="icon-book"></i>
          教育资源平台
        </router-link>
      </div>
      
      <div class="navbar-menu">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/resources" class="nav-item">资源</router-link>
        
        <template v-if="isLoggedIn">
          <router-link v-if="isAdmin" to="/admin" class="nav-item">用户管理</router-link>
          <router-link v-if="canUpload" to="/upload" class="nav-item">上传</router-link>
          <router-link to="/my-resources" class="nav-item">我的资源</router-link>
          <router-link to="/my-collections" class="nav-item">我的收藏</router-link>
          
          <div class="nav-dropdown">
            <div class="nav-item dropdown-toggle" @click="toggleDropdown">
              <img :src="user.avatar || defaultAvatar" class="user-avatar" alt="用户">
              <span>{{ user.nickname || user.username }}</span>
              <i class="icon-chevron-down"></i>
            </div>
            <div class="dropdown-menu" :class="{ show: showDropdown }">
              <router-link to="/user-center" class="dropdown-item">
                <i class="icon-user"></i> 用户中心
              </router-link>
              <a href="#" class="dropdown-item" @click.prevent="handleLogout">
                <i class="icon-logout"></i> 退出登录
              </a>
            </div>
          </div>
        </template>
        
        <template v-else>
          <router-link to="/login" class="nav-item">登录</router-link>
          <router-link to="/register" class="nav-item btn-register">注册</router-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

export default {
  name: 'NavBar',
  setup() {
    const store = useStore()
    const router = useRouter()
    const showDropdown = ref(false)
    
    const isLoggedIn = computed(() => store.getters.isLoggedIn)
    const user = computed(() => store.state.user)
    const canUpload = computed(() => store.getters.canUpload)
    const isAdmin = computed(() => user.value && user.value.role === 'admin')
    
    const defaultAvatar = '/default-avatar.png'
    
    const toggleDropdown = () => {
      showDropdown.value = !showDropdown.value
    }
    
    const handleLogout = () => {
      store.dispatch('logout')
      router.push('/login')
    }
    
    return {
      isLoggedIn,
      user,
      canUpload,
      isAdmin,
      showDropdown,
      defaultAvatar,
      toggleDropdown,
      handleLogout
    }
  }
}
</script>

<style lang="less" scoped>
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
  
  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 64px;
  }
  
  .navbar-brand {
    .logo {
      display: flex;
      align-items: center;
      font-size: 20px;
      font-weight: bold;
      color: white;
      text-decoration: none;
      
      .icon-book {
        margin-right: 10px;
        font-size: 24px;
      }
    }
  }
  
  .navbar-menu {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .nav-item {
      color: rgba(255, 255, 255, 0.9);
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 4px;
      transition: all 0.3s;
      font-size: 14px;
      
      &:hover, &.router-link-active {
        background: rgba(255, 255, 255, 0.2);
        color: white;
      }
      
      &.btn-register {
        background: white;
        color: #667eea;
        font-weight: 500;
        
        &:hover {
          background: #f0f0f0;
        }
      }
    }
    
    .nav-dropdown {
      position: relative;
      
      .dropdown-toggle {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        
        .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          object-fit: cover;
          border: 2px solid white;
        }
        
        .icon-chevron-down {
          font-size: 12px;
        }
      }
      
      .dropdown-menu {
        position: absolute;
        top: 100%;
        right: 0;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        min-width: 180px;
        padding: 8px 0;
        display: none;
        margin-top: 8px;
        
        &.show {
          display: block;
        }
        
        .dropdown-item {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 10px 20px;
          color: #333;
          text-decoration: none;
          transition: background 0.2s;
          
          &:hover {
            background: #f5f5f5;
          }
          
          i {
            font-size: 16px;
            color: #666;
          }
        }
      }
    }
  }
}
</style>
