import { ref } from 'vue'

const DEFAULT_LOCALE = 'zh-CN'

const messages = {
  'zh-CN': {
    comments: '评论',
    reply: '回复',
    send: '发送',
    cancel: '取消',
    loadMore: '加载更多',
    expandReplies: '展开更多回复',
    collapseReplies: '收起回复',
    writeComment: '写下你的评论...',
    replyTo: '回复 @{name}...',
    uploading: '上传中...',
    networkError: '网络错误，请稍后重试'
  },
  'en-US': {
    comments: 'Comments',
    reply: 'Reply',
    send: 'Send',
    cancel: 'Cancel',
    loadMore: 'Load more',
    expandReplies: 'View more replies',
    collapseReplies: 'Hide replies',
    writeComment: 'Write a comment...',
    replyTo: 'Reply @{name}...',
    uploading: 'Uploading...',
    networkError: 'Network error, please retry'
  }
}

export const locale = ref(localStorage.getItem('locale') || DEFAULT_LOCALE)

export function setLocale(next) {
  locale.value = next
  localStorage.setItem('locale', next)
}

export function t(key, params = {}) {
  const dict = messages[locale.value] || messages[DEFAULT_LOCALE]
  let text = dict[key] || key
  Object.keys(params).forEach(k => {
    text = text.replaceAll(`@{${k}}`, String(params[k]))
  })
  return text
}

