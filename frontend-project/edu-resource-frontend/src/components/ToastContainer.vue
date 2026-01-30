<template>
  <teleport to="body">
    <transition-group name="toast" tag="div" class="toast-container">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="[`toast-${toast.type}`, { 'toast-closable': toast.closable }]"
      >
        <div class="toast-icon">
          <i :class="getIcon(toast.type)"></i>
        </div>
        <div class="toast-content">
          <h4 v-if="toast.title" class="toast-title">{{ toast.title }}</h4>
          <p class="toast-message">{{ toast.message }}</p>
        </div>
        <button
          v-if="toast.closable"
          class="toast-close"
          @click="removeToast(toast.id)"
        >
          <i class="icon-close"></i>
        </button>
      </div>
    </transition-group>
  </teleport>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'ToastContainer',
  setup() {
    const toasts = ref([])
    let toastId = 0

    const addToast = (options) => {
      const id = ++toastId
      const toast = {
        id,
        type: options.type || 'info',
        title: options.title || '',
        message: options.message || '',
        duration: options.duration !== undefined ? options.duration : 3000,
        closable: options.closable !== undefined ? options.closable : true
      }

      toasts.value.push(toast)

      if (toast.duration > 0) {
        setTimeout(() => {
          removeToast(id)
        }, toast.duration)
      }

      return id
    }

    const removeToast = (id) => {
      const index = toasts.value.findIndex(t => t.id === id)
      if (index !== -1) {
        toasts.value.splice(index, 1)
      }
    }

    const getIcon = (type) => {
      const icons = {
        success: 'icon-check-circle',
        error: 'icon-error-circle',
        warning: 'icon-warning',
        info: 'icon-info-circle'
      }
      return icons[type] || icons.info
    }

    return {
      toasts,
      addToast,
      removeToast,
      getIcon
    }
  }
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 300px;
  max-width: 450px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideIn 0.3s ease-out;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.toast-success {
  border-left: 4px solid #10b981;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-warning {
  border-left: 4px solid #f59e0b;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}

.toast-icon {
  flex-shrink: 0;
  font-size: 24px;
}

.toast-success .toast-icon {
  color: #10b981;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-warning .toast-icon {
  color: #f59e0b;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.toast-message {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.toast-close {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.toast-close:hover {
  color: #6b7280;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
