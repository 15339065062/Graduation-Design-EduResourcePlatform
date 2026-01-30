<template>
  <div class="friends-page">
    <div class="container">
      <div class="header">
        <h2>好友</h2>
        <div class="sub">你关注的用户</div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!list.length" class="empty">暂无好友，去关注一些人吧</div>
      <div v-else class="list">
        <div v-for="u in list" :key="u.id" class="item">
          <router-link class="left" :to="`/user/${u.id}`">
            <img v-if="u.avatar" :src="u.avatar" alt="" class="avatar" />
            <div v-else class="avatar avatar-ph" aria-hidden="true">{{ (u.nickname || u.username || '用户').slice(0, 1) }}</div>
            <div class="info">
              <div class="name">{{ u.nickname || u.username }}</div>
              <div class="sub">@{{ u.username }}</div>
            </div>
          </router-link>
          <button class="btn btn-secondary btn-sm" type="button" @click="goChat(u.id)">私信</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { followApi } from '@/api/follow-api'

export default {
  name: 'Friends',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const list = ref([])

    const load = async () => {
      loading.value = true
      try {
        const res = await followApi.listFollowing({ page: 1, pageSize: 200 })
        if (res.success) {
          list.value = res.data.list || []
        }
      } finally {
        loading.value = false
      }
    }

    const goChat = (userId) => router.push(`/chat/${userId}`)

    onMounted(load)

    return {
      loading,
      list,
      goChat
    }
  }
}
</script>

<style scoped lang="less">
.friends-page {
  padding: 24px 0 56px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.sub {
  color: var(--text-3);
  font-size: 13px;
}

.list {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  transition: background var(--transition);
}

.item:hover {
  background: rgba(15, 23, 42, 0.04);
}

.left {
  display: flex;
  gap: 12px;
  align-items: center;
  text-decoration: none;
  color: inherit;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(79, 109, 255, 0.12);
  color: rgba(15, 23, 42, 0.82);
  font-weight: 800;
  font-size: 16px;
}

.name {
  font-weight: 700;
}

.sub {
  color: var(--text-3);
  font-size: 12px;
}
</style>

