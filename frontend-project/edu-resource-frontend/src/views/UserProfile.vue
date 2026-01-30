<template>
  <div class="profile-page">
    <div class="container">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!profile" class="empty">用户不存在</div>
      <div v-else class="profile-card">
        <div class="header">
          <img v-if="profile.user.avatar" :src="profile.user.avatar" class="avatar" alt="" />
          <div v-else class="avatar avatar-ph" aria-hidden="true">{{ initials }}</div>
          <div class="info">
            <div class="name">{{ profile.user.nickname || profile.user.username }}</div>
            <div class="sub">@{{ profile.user.username }} · {{ roleLabel }}</div>
            <div class="stats">
              <span>粉丝 {{ profile.followerCount }}</span>
              <span>关注 {{ profile.followingCount }}</span>
            </div>
          </div>
          <div v-if="canOperate" class="actions">
            <button class="btn btn-light" type="button" @click="toggleFollow" :disabled="opLoading">
              {{ profile.isFollowing ? '已关注' : '关注' }}
            </button>
            <button class="btn btn-primary" type="button" @click="goChat">私信</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '@/api/user-api'
import { followApi } from '@/api/follow-api'

export default {
  name: 'UserProfile',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()

    const loading = ref(false)
    const opLoading = ref(false)
    const profile = ref(null)

    const currentUserId = computed(() => store.state.user?.id || null)
    const isLoggedIn = computed(() => !!store.state.token)
    const userId = computed(() => parseInt(route.params.id))

    const canOperate = computed(() => isLoggedIn.value && currentUserId.value && currentUserId.value !== userId.value)

    const initials = computed(() => {
      const s = profile.value?.user?.nickname || profile.value?.user?.username || '用户'
      return s.slice(0, 1)
    })

    const roleLabel = computed(() => {
      const r = profile.value?.user?.role
      if (r === 'admin') return '管理员'
      if (r === 'teacher') return '老师'
      if (r === 'student') return '学生'
      return '用户'
    })

    const load = async () => {
      loading.value = true
      try {
        const res = await userApi.getPublicProfile(userId.value)
        if (res.success) {
          profile.value = res.data
        } else {
          profile.value = null
        }
      } finally {
        loading.value = false
      }
    }

    const toggleFollow = async () => {
      if (!canOperate.value || opLoading.value) return
      opLoading.value = true
      try {
        if (profile.value.isFollowing) {
          const res = await followApi.unfollow(userId.value)
          if (res.success) {
            profile.value.isFollowing = false
            profile.value.followerCount = Math.max(0, (profile.value.followerCount || 0) - 1)
          }
        } else {
          const res = await followApi.follow(userId.value)
          if (res.success) {
            profile.value.isFollowing = true
            profile.value.followerCount = (profile.value.followerCount || 0) + 1
          }
        }
      } finally {
        opLoading.value = false
      }
    }

    const goChat = () => {
      router.push(`/chat/${userId.value}`)
    }

    watch(() => route.params.id, load)
    onMounted(load)

    return {
      loading,
      opLoading,
      profile,
      initials,
      roleLabel,
      canOperate,
      toggleFollow,
      goChat
    }
  }
}
</script>

<style scoped lang="less">
.profile-page {
  min-height: 60vh;
  padding: 20px 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.profile-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 18px;
}

.header {
  display: flex;
  gap: 14px;
  align-items: center;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  flex: 0 0 auto;
}

.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 800;
  font-size: 22px;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.sub {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.stats {
  margin-top: 8px;
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #111827;
}

.actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 14px;
}

.btn-light {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.btn-primary {
  background: #667eea;
  color: #fff;
}
</style>

