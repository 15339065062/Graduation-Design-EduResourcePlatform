import request from '@/utils/request'

export const commentApi = {
  getComments(resourceId, params) {
    return request.get('/comment', {
      params: {
        resourceId,
        ...params
      }
    })
  },

  getCommentDetail(id) {
    return request.get(`/comment/${id}`)
  },

  createComment(data) {
    return request.post('/comment', data)
  },

  updateComment(id, data) {
    return request.put(`/comment/${id}`, data)
  },

  deleteComment(id) {
    return request.delete(`/comment/${id}`)
  }
}