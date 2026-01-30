<template>
  <div class="messages-page">
    <div class="container">
      <div class="header">
        <h2>私信</h2>
        <div class="sub">最近聊天</div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!list.length" class="empty">暂无对话，去私信一个人吧</div>
      <div v-else class="list">
        <div v-for="c in list" :key="c.id" class="item" @click="goChat(c.otherUser.id)">
          <div class="left">
            <img v-if="c.otherUser.avatar" :src="c.otherUser.avatar" alt="" class="avatar" />
            <div v-else class="avatar avatar-ph" aria-hidden="true">{{ (c.otherUser.nickname || c.otherUser.username || '用户').slice(0, 1) }}</div>
            <div class="info">
              <div class="top">
                <div class="name">{{ c.otherUser.nickname || c.otherUser.username }}</div>
                <div class="time">{{ formatTime(c.lastMessageTime) }}</div>
              </div>
              <div class="preview">{{ c.lastMessagePreview || '' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api/chat-api'

export default {
  name: 'Messages',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const list = ref([])

    const load = async () => {
      loading.value = true
      try {
        const res = await chatApi.listConversations({ page: 1, pageSize: 100 })
        if (res.success) {
          list.value = res.data.list || []
        }
      } finally {
        loading.value = false
      }
    }

    const goChat = (userId) => router.push(`/chat/${userId}`)

    const formatTime = (ts) => {
      if (!ts) return ''
      const d = new Date(Number(ts))
      return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    }

    onMounted(load)

    return {
      loading,
      list,
      goChat,
      formatTime
    }
  }
}
</script>

<style scoped lang="less">
.messages-page {
  padding: 20px 0;
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
  color: #6b7280;
  font-size: 13px;
}

.item {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
}

.left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.avatar {
  width: 46px;
  height: 46px;
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
  font-size: 16px;
}

.info {
  flex: 1;
  min-width: 0;
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.name {
  font-weight: 800;
  color: #111827;
}

.time {
  color: #9ca3af;
  font-size: 12px;
}

.preview {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>

