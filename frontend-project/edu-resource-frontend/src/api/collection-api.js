import request from '../utils/request'

export const collectionApi = {
  toggleCollection(resourceId) {
    return request.post('/collection/toggle', { resourceId })
  },

  getCollections(page = 1, pageSize = 20) {
    return request.get('/collection/list', { params: { page, pageSize } })
  },

  checkStatus(resourceId) {
    return request.get(`/collection/status?resourceId=${resourceId}`)
  }
}
