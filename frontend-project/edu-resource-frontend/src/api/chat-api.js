import request from '@/utils/request'
import store from '@/store'

export const chatApi = {
  listConversations(params) {
    return request.get('/chat/conversations', { params })
  },
  getConversationWith(userId) {
    return request.get('/chat/conversation-with', { params: { userId } })
  },
  listMessages(conversationId, params) {
    return request.get('/chat/messages', { params: { conversationId, ...params } })
  },
  sendText(toUserId, text) {
    return request.post('/chat/send', { toUserId, msgType: 'text', text })
  },
  sendMedia(toUserId, msgType, file, text = '') {
    const fd = new FormData()
    fd.append('toUserId', String(toUserId))
    fd.append('msgType', msgType)
    fd.append('text', text || '')
    fd.append('file', file)
    return request.post('/chat/send', fd, { timeout: 120000 })
  },
  mediaUrl(path) {
    const t = store.state.token || ''
    if (!t) return path
    const sep = path.includes('?') ? '&' : '?'
    return `${path}${sep}token=${encodeURIComponent(t)}`
  }
}

