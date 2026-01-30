import request from '@/utils/request'

export const followApi = {
  follow(userId) {
    return request.post(`/follow/${userId}`)
  },
  unfollow(userId) {
    return request.delete(`/follow/${userId}`)
  },
  getStatus(userId) {
    return request.get('/follow/status', { params: { userId } })
  },
  listFollowing(params) {
    return request.get('/follow/following', { params })
  },
  listFollowers(params) {
    return request.get('/follow/followers', { params })
  }
}

