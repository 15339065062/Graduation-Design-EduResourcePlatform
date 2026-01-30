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
          <router-link to="/messages" class="nav-item">私信</router-link>
          <router-link to="/friends" class="nav-item">好友</router-link>
          <router-link v-if="isAdmin" to="/admin" class="nav-item">用户管理</router-link>
          <router-link v-if="canUpload" to="/upload" class="nav-item">上传</router-link>
          <a v-else href="#" class="nav-item nav-item-disabled" @click.prevent="goApplyUpload">上传</a>
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

    const goApplyUpload = () => {
      router.push({ path: '/user-center', query: { tab: 'role-change' } })
    }
    
    return {
      isLoggedIn,
      user,
      canUpload,
      isAdmin,
      showDropdown,
      defaultAvatar,
      toggleDropdown,
      handleLogout,
      goApplyUpload
    }
  }
}
</script>

<style lang="less" scoped>
.navbar {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
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
    height: 68px;
  }
  
  .navbar-brand {
    .logo {
      display: flex;
      align-items: center;
      font-size: 20px;
      font-weight: bold;
      color: rgba(15, 23, 42, 0.92);
      text-decoration: none;
      
      .icon-book {
        margin-right: 10px;
        font-size: 24px;
        color: #4f6dff;
      }
    }
  }
  
  .navbar-menu {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .nav-item {
      color: rgba(15, 23, 42, 0.70);
      text-decoration: none;
      padding: 9px 14px;
      border-radius: 12px;
      transition: background 220ms cubic-bezier(0.16, 1, 0.3, 1), color 220ms cubic-bezier(0.16, 1, 0.3, 1), transform 220ms cubic-bezier(0.16, 1, 0.3, 1);
      font-size: 14px;
      
      &:hover, &.router-link-active {
        background: rgba(15, 23, 42, 0.06);
        color: rgba(15, 23, 42, 0.92);
      }
      
      &.btn-register {
        background: linear-gradient(135deg, #4f6dff 0%, #7b5cff 100%);
        color: white;
        font-weight: 600;
        box-shadow: 0 10px 24px rgba(79, 109, 255, 0.18);
        
        &:hover {
          transform: translateY(-1px);
        }
      }
    }

    .nav-item-disabled {
      opacity: 0.78;
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
          border: 2px solid rgba(15, 23, 42, 0.10);
        }
        
        .icon-chevron-down {
          font-size: 12px;
          color: rgba(15, 23, 42, 0.54);
        }
      }
      
      .dropdown-menu {
        position: absolute;
        top: 100%;
        right: 0;
        background: rgba(255, 255, 255, 0.92);
        backdrop-filter: blur(12px);
        border-radius: 14px;
        border: 1px solid rgba(15, 23, 42, 0.10);
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
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
          color: rgba(15, 23, 42, 0.86);
          text-decoration: none;
          transition: background 220ms cubic-bezier(0.16, 1, 0.3, 1);
          
          &:hover {
            background: rgba(15, 23, 42, 0.06);
          }
          
          i {
            font-size: 16px;
            color: rgba(15, 23, 42, 0.54);
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .navbar {
    .container {
      padding: 0 14px;
      height: 64px;
    }
    .navbar-menu {
      gap: 8px;
      .nav-item {
        padding: 8px 10px;
      }
      .nav-dropdown {
        .dropdown-toggle {
          span {
            display: none;
          }
        }
      }
    }
  }
}
</style>
