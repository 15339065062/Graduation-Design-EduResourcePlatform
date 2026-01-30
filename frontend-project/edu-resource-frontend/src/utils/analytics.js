import request from '@/utils/request'

export function trackEvent(eventName, properties = {}) {
  const enabled = localStorage.getItem('analyticsEnabled')
  if (enabled === '0') return Promise.resolve()
  return request.post('/analytics/event', { eventName, properties }).catch(() => {})
}

