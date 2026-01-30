<template>
  <div class="chat-page">
    <div class="container">
      <div class="chat-header">
        <button class="back" type="button" @click="goBack">返回</button>
        <div class="title">
          {{ otherUser?.nickname || otherUser?.username || '私信' }}
        </div>
        <router-link v-if="otherUser" class="profile" :to="`/user/${otherUser.id}`">主页</router-link>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="chat-body">
        <div class="msg-list" ref="listRef">
          <button v-if="hasMore" class="load-more" type="button" @click="loadMore" :disabled="loadingMore">加载更多</button>

          <div v-for="m in messages" :key="m.id" class="msg-row" :class="{ mine: m.senderId === currentUserId }">
            <img
              v-if="m.sender?.avatar"
              class="avatar"
              :src="m.sender.avatar"
              alt=""
            />
            <div v-else class="avatar avatar-ph" aria-hidden="true">{{ senderInitial(m) }}</div>

            <div class="bubble-wrap">
              <div class="time">{{ formatTime(m.createTime) }}</div>
              <div class="bubble">
                <template v-if="m.msgType === 'text'">
                  <div class="text" v-html="renderText(m.text)"></div>
                </template>
                <template v-else-if="m.msgType === 'image'">
                  <img class="img" :src="media(m.thumbUrl || m.mediaUrl)" alt="" @click="open(media(m.mediaUrl))" />
                </template>
                <template v-else-if="m.msgType === 'video'">
                  <video class="video" controls :src="media(m.mediaUrl)"></video>
                </template>
                <template v-else-if="m.msgType === 'audio'">
                  <audio controls :src="media(m.mediaUrl)"></audio>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="composer">
          <input ref="imgInput" type="file" accept="image/jpeg,image/png" style="display:none" @change="onPickFile('image', $event)" />
          <input ref="videoInput" type="file" accept="video/mp4,video/webm,video/ogg" style="display:none" @change="onPickFile('video', $event)" />
          <input ref="audioInput" type="file" accept="audio/mpeg,audio/mp3,audio/wav,audio/ogg" style="display:none" @change="onPickFile('audio', $event)" />

          <div class="tools">
            <button class="tool" type="button" @click="openPicker('image')">图</button>
            <button class="tool" type="button" @click="openPicker('video')">视</button>
            <button class="tool" type="button" @click="toggleVoice">{{ voiceMode ? '键' : '音' }}</button>
          </div>
          <template v-if="voiceMode">
            <button
              class="record-btn"
              type="button"
              :class="{ recording: isRecording, cancel: willCancel }"
              @pointerdown="onRecordDown"
              @pointermove="onRecordMove"
              @pointerup="onRecordUp"
              @pointercancel="onRecordCancel"
            >
              {{ recordLabel }}
            </button>
          </template>
          <template v-else>
            <textarea v-model="text" class="input" rows="1" placeholder="发消息..." maxlength="2000"></textarea>
            <button class="send" type="button" @click="sendText" :disabled="sending || !text.trim()">发送</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { chatApi } from '@/api/chat-api'
import { userApi } from '@/api/user-api'

export default {
  name: 'Chat',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()

    const currentUserId = computed(() => store.state.user?.id || null)
    const otherId = computed(() => parseInt(route.params.userId))

    const loading = ref(false)
    const sending = ref(false)
    const loadingMore = ref(false)
    const conversationId = ref(null)
    const otherUser = ref(null)

    const messages = ref([])
    const page = ref(1)
    const pageSize = 30
    const total = ref(0)
    const hasMore = computed(() => messages.value.length < total.value)

    const text = ref('')
    const listRef = ref(null)
    const imgInput = ref(null)
    const videoInput = ref(null)
    const audioInput = ref(null)

    const voiceMode = ref(false)
    const isRecording = ref(false)
    const willCancel = ref(false)
    const recordSeconds = ref(0)
    const recordLabel = computed(() => {
      if (!isRecording.value) return '按住 说话'
      if (willCancel.value) return `松开 取消 ${recordSeconds.value}s`
      return `松开 发送 ${recordSeconds.value}s`
    })
    let recordStartY = 0
    let recordPointerId = null
    let recordStream = null
    let recorder = null
    let recordChunks = []
    let recordTimer = null

    const media = (url) => (url ? chatApi.mediaUrl(url) : '')

    const renderText = (s) => String(s || '').replaceAll('\n', '<br/>')

    const senderInitial = (m) => {
      const s = m.sender?.nickname || m.sender?.username || '用户'
      return s.slice(0, 1)
    }

    const formatTime = (ts) => {
      if (!ts) return ''
      const d = new Date(Number(ts))
      return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    }

    const open = (url) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    }

    const loadMeta = async () => {
      const [pRes, cRes] = await Promise.all([
        userApi.getPublicProfile(otherId.value),
        chatApi.getConversationWith(otherId.value)
      ])
      if (pRes.success) otherUser.value = pRes.data.user
      if (cRes.success) conversationId.value = cRes.data.conversationId
    }

    const loadMessages = async (reset = false) => {
      if (!conversationId.value) return
      const p = reset ? 1 : page.value
      const res = await chatApi.listMessages(conversationId.value, { page: p, pageSize })
      if (res.success) {
        total.value = res.data.total || 0
        const list = (res.data.list || []).slice().reverse()
        if (reset) {
          messages.value = list
        } else {
          messages.value = list.concat(messages.value)
        }
      }
    }

    const scrollToBottom = async () => {
      await nextTick()
      const el = listRef.value
      if (!el) return
      el.scrollTop = el.scrollHeight
    }

    const init = async () => {
      loading.value = true
      try {
        await loadMeta()
        page.value = 1
        await loadMessages(true)
        await scrollToBottom()
      } finally {
        loading.value = false
      }
    }

    const loadMore = async () => {
      if (loadingMore.value || !hasMore.value) return
      loadingMore.value = true
      try {
        page.value += 1
        await loadMessages(false)
      } finally {
        loadingMore.value = false
      }
    }

    const sendText = async () => {
      if (!text.value.trim() || sending.value) return
      sending.value = true
      try {
        const res = await chatApi.sendText(otherId.value, text.value)
        if (res.success) {
          const m = res.data
          m.createTime = Date.now()
          messages.value.push({
            ...m,
            sender: store.state.user
          })
          text.value = ''
          await scrollToBottom()
        }
      } finally {
        sending.value = false
      }
    }

    const openPicker = (type) => {
      if (type === 'image') imgInput.value && imgInput.value.click()
      if (type === 'video') videoInput.value && videoInput.value.click()
      if (type === 'audio') audioInput.value && audioInput.value.click()
    }

    const onPickFile = async (msgType, e) => {
      const file = (e.target.files || [])[0]
      e.target.value = ''
      if (!file || sending.value) return
      sending.value = true
      try {
        const res = await chatApi.sendMedia(otherId.value, msgType, file, '')
        if (res.success) {
          const m = res.data
          m.createTime = Date.now()
          messages.value.push({
            ...m,
            sender: store.state.user
          })
          await scrollToBottom()
        }
      } finally {
        sending.value = false
      }
    }

    const toggleVoice = () => {
      if (sending.value) return
      voiceMode.value = !voiceMode.value
    }

    const stopRecordTimer = () => {
      if (recordTimer) {
        clearInterval(recordTimer)
        recordTimer = null
      }
    }

    const cleanupRecorder = () => {
      stopRecordTimer()
      if (recorder) {
        try { recorder.ondataavailable = null } catch (e) {}
        try { recorder.onstop = null } catch (e) {}
      }
      recorder = null
      recordChunks = []
      if (recordStream) {
        try { recordStream.getTracks().forEach(t => t.stop()) } catch (e) {}
      }
      recordStream = null
      isRecording.value = false
      willCancel.value = false
      recordSeconds.value = 0
      recordPointerId = null
    }

    const pickRecordMime = () => {
      const cands = [
        'audio/webm;codecs=opus',
        'audio/ogg;codecs=opus',
        'audio/webm',
        'audio/ogg'
      ]
      const MR = window.MediaRecorder
      if (!MR || !MR.isTypeSupported) return ''
      for (const m of cands) {
        if (MR.isTypeSupported(m)) return m
      }
      return ''
    }

    const startRecording = async () => {
      if (isRecording.value || sending.value) return
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
        alert('当前浏览器不支持录音')
        return
      }
      isRecording.value = true
      willCancel.value = false
      recordSeconds.value = 0
      recordChunks = []

      try {
        recordStream = await navigator.mediaDevices.getUserMedia({ audio: true })
        const mimeType = pickRecordMime()
        recorder = mimeType ? new MediaRecorder(recordStream, { mimeType }) : new MediaRecorder(recordStream)
        recorder.ondataavailable = (ev) => {
          if (ev.data && ev.data.size > 0) recordChunks.push(ev.data)
        }
        recorder.start()
        recordTimer = setInterval(() => {
          recordSeconds.value += 1
          if (recordSeconds.value >= 60) {
            stopRecordingAndSend()
          }
        }, 1000)
      } catch (e) {
        cleanupRecorder()
        alert('无法获取麦克风权限')
      }
    }

    const stopRecordingAndSend = async () => {
      if (!recorder || recorder.state === 'inactive') {
        cleanupRecorder()
        return
      }
      stopRecordTimer()
      const localWillCancel = willCancel.value

      await new Promise((resolve) => {
        recorder.onstop = resolve
        try { recorder.stop() } catch (e) { resolve() }
      })

      const chunks = recordChunks.slice()
      const mime = (recorder && recorder.mimeType) ? recorder.mimeType : ''
      const secs = recordSeconds.value
      const stream = recordStream
      recorder = null
      recordStream = null

      if (stream) {
        try { stream.getTracks().forEach(t => t.stop()) } catch (e) {}
      }

      isRecording.value = false
      willCancel.value = false
      recordSeconds.value = 0
      recordPointerId = null
      recordChunks = []

      if (localWillCancel || secs < 1 || !chunks.length) return

      const blob = new Blob(chunks, { type: mime || 'audio/webm' })
      const ext = (mime || '').includes('ogg') ? 'ogg' : 'webm'
      const file = new File([blob], `voice_${Date.now()}.${ext}`, { type: blob.type })
      sending.value = true
      try {
        const res = await chatApi.sendMedia(otherId.value, 'audio', file, '')
        if (res.success) {
          const m = res.data
          m.createTime = Date.now()
          messages.value.push({
            ...m,
            sender: store.state.user
          })
          await scrollToBottom()
        }
      } finally {
        sending.value = false
      }
    }

    const onRecordDown = async (e) => {
      if (!voiceMode.value) return
      if (sending.value) return
      try { e.currentTarget.setPointerCapture(e.pointerId) } catch (err) {}
      recordPointerId = e.pointerId
      recordStartY = e.clientY
      await startRecording()
    }

    const onRecordMove = (e) => {
      if (!isRecording.value) return
      if (recordPointerId !== e.pointerId) return
      const dy = recordStartY - e.clientY
      willCancel.value = dy > 80
    }

    const onRecordUp = async (e) => {
      if (!isRecording.value) return
      if (recordPointerId !== e.pointerId) return
      await stopRecordingAndSend()
    }

    const onRecordCancel = async (e) => {
      if (!isRecording.value) return
      if (recordPointerId !== e.pointerId) return
      willCancel.value = true
      await stopRecordingAndSend()
    }

    const goBack = () => router.back()

    onMounted(init)
    onUnmounted(() => {
      cleanupRecorder()
    })

    return {
      loading,
      sending,
      loadingMore,
      otherUser,
      currentUserId,
      messages,
      hasMore,
      text,
      listRef,
      imgInput,
      videoInput,
      audioInput,
      voiceMode,
      isRecording,
      willCancel,
      recordLabel,
      media,
      renderText,
      senderInitial,
      formatTime,
      open,
      openPicker,
      onPickFile,
      toggleVoice,
      onRecordDown,
      onRecordMove,
      onRecordUp,
      onRecordCancel,
      sendText,
      loadMore,
      goBack
    }
  }
}
</script>

<style scoped lang="less">
.chat-page {
  padding: 18px 0 56px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 12px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.back, .profile {
  border: none;
  background: transparent;
  color: #667eea;
  cursor: pointer;
  text-decoration: none;
}

.title {
  font-weight: 800;
}

.chat-body {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.msg-list {
  height: 62vh;
  overflow: auto;
  padding: 14px 12px;
  background: rgba(15, 23, 42, 0.02);
}

.load-more {
  width: 100%;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.82);
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 10px;
  transition: background var(--transition), border-color var(--transition), transform var(--transition);
}

.load-more:hover {
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(15, 23, 42, 0.16);
}

.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin: 10px 0;
}

.msg-row.mine {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex: 0 0 auto;
}

.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(79, 109, 255, 0.12);
  color: rgba(15, 23, 42, 0.82);
  font-weight: 800;
  font-size: 13px;
}

.bubble-wrap {
  max-width: 72%;
}

.time {
  font-size: 11px;
  color: var(--text-3);
  margin: 0 6px 4px;
}

.bubble {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 14px;
  padding: 10px 12px;
  box-shadow: var(--shadow-sm);
}

.msg-row.mine .bubble {
  background: linear-gradient(135deg, rgba(79, 109, 255, 0.96) 0%, rgba(123, 92, 255, 0.96) 100%);
  border-color: rgba(79, 109, 255, 0.30);
  color: #fff;
  box-shadow: 0 10px 24px rgba(79, 109, 255, 0.18);
}

.text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
}

.img {
  max-width: 220px;
  border-radius: 12px;
  display: block;
  cursor: pointer;
}

.video {
  width: 260px;
  border-radius: 12px;
}

.composer {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
}

.tools {
  display: flex;
  gap: 6px;
}

.tool {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(15, 23, 42, 0.05);
  cursor: pointer;
  transition: background var(--transition), border-color var(--transition), transform var(--transition);
}

.tool:hover {
  background: rgba(15, 23, 42, 0.07);
  border-color: rgba(15, 23, 42, 0.16);
  transform: translateY(-1px);
}

.input {
  flex: 1;
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 12px;
  padding: 8px 10px;
  resize: none;
  background: rgba(255, 255, 255, 0.92);
}

.send {
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: linear-gradient(135deg, rgba(79, 109, 255, 0.96) 0%, rgba(123, 92, 255, 0.96) 100%);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(79, 109, 255, 0.18);
  transition: transform var(--transition), box-shadow var(--transition);
}

.send:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(79, 109, 255, 0.22);
}

.send:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.record-btn {
  flex: 1;
  height: 40px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}

.record-btn.recording {
  background: rgba(15, 23, 42, 0.92);
  color: #fff;
  border-color: rgba(15, 23, 42, 0.92);
}

.record-btn.recording.cancel {
  background: #ef4444;
  border-color: #ef4444;
}
</style>

