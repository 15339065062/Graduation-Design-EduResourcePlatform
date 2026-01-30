import request from '@/utils/request'

export const commentApiV2 = {
  listRoot(resourceId, params) {
    return request.get('/comment/v2', { params: { resourceId, ...params } })
  },
  listReplies(parentId, params) {
    return request.get(`/comment/v2/${parentId}/replies`, { params })
  },
  createComment(formData) {
    return request.post('/comment/v2', formData, {
      timeout: 120000
    })
  },
  updateComment(id, data) {
    return request.put(`/comment/v2/${id}`, data)
  },
  deleteComment(id) {
    return request.delete(`/comment/v2/${id}`)
  }
}

