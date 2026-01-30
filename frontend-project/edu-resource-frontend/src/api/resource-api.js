import request from '@/utils/request'

export const resourceApi = {
  getResources(params) {
    return request.get('/resources', { params })
  },

  getResourceDetail(id) {
    return request.get(`/resources/${id}`)
  },

  uploadResource(data, onProgress) {
    return request.post('/resources', data, {
      timeout: 120000,
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  },

  updateResource(id, data) {
    return request.put(`/resources/${id}`, data)
  },

  deleteResource(id) {
    return request.delete(`/resources/${id}`)
  },

  downloadResource(id) {
    return request.get(`/resources/${id}/download`, {
      responseType: 'blob'
    })
  },

  collectResource(id) {
    return request.post(`/resources/${id}/collect`)
  },

  uncollectResource(id) {
    return request.delete(`/resources/${id}/collect`)
  },

  checkCollection(id) {
    return request.get(`/resources/${id}/collect`)
  },

  getMyResources(params) {
    return request.get('/resources/my', { params })
  },

  getCollections(params) {
    return request.get('/resources/collections', { params })
  },

  getCategories() {
    return request.get('/resources/categories')
  },

  getStats() {
    return request.get('/resources/stats')
  },

  getRelatedResources(id) {
    return request.get(`/resources/${id}/related`)
  }
}
