<template>
  <div class="pagination">
    <button 
      class="pagination-btn" 
      :disabled="currentPage === 1" 
      @click="handlePageChange(currentPage - 1)"
    >
      <i class="icon-chevron-left"></i> Previous
    </button>
    
    <div class="pagination-pages">
      <button 
        v-for="page in displayedPages" 
        :key="page"
        class="pagination-page"
        :class="{ active: page === currentPage }"
        @click="handlePageChange(page)"
      >
        {{ page }}
      </button>
    </div>
    
    <button 
      class="pagination-btn" 
      :disabled="currentPage === totalPages" 
      @click="handlePageChange(currentPage + 1)"
    >
      Next <i class="icon-chevron-right"></i>
    </button>
    
    <div class="pagination-info">
      <span>Total: {{ totalItems }} items</span>
      <span>Page {{ currentPage }} of {{ totalPages }}</span>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'PagePagination',
  props: {
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    totalItems: {
      type: Number,
      default: 0
    },
    maxVisiblePages: {
      type: Number,
      default: 5
    }
  },
  emits: ['page-change'],
  
  setup(props, { emit }) {
    const totalPages = computed(() => {
      return Math.ceil(props.totalItems / props.pageSize) || 1
    })
    
    const displayedPages = computed(() => {
      const pages = []
      const { currentPage, maxVisiblePages } = props
      const total = totalPages.value
      
      let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2))
      let endPage = Math.min(total, startPage + maxVisiblePages - 1)
      
      if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(1, endPage - maxVisiblePages + 1)
      }
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i)
      }
      
      return pages
    })
    
    const handlePageChange = (page) => {
      if (page >= 1 && page <= totalPages.value && page !== props.currentPage) {
        emit('page-change', page)
      }
    }
    
    return {
      totalPages,
      displayedPages,
      handlePageChange
    }
  }
}
</script>

<style lang="less" scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  
  .pagination-btn {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    border: 1px solid #ddd;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
    font-size: 14px;
    color: #333;
    
    &:hover:not(:disabled) {
      background: #667eea;
      color: white;
      border-color: #667eea;
    }
    
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
  
  .pagination-pages {
    display: flex;
    gap: 5px;
    
    .pagination-page {
      min-width: 36px;
      height: 36px;
      border: 1px solid #ddd;
      background: white;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;
      font-size: 14px;
      color: #333;
      display: flex;
      align-items: center;
      justify-content: center;
      
      &:hover {
        background: #f0f0f0;
      }
      
      &.active {
        background: #667eea;
        color: white;
        border-color: #667eea;
      }
    }
  }
  
  .pagination-info {
    display: flex;
    gap: 15px;
    margin-left: 20px;
    font-size: 14px;
    color: #666;
  }
}
</style>
