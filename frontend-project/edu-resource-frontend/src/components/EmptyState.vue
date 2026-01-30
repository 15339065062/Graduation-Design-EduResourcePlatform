<template>
  <div class="empty-state" :class="size">
    <div class="empty-icon">
      <i :class="icon"></i>
    </div>
    <h3 v-if="title" class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <slot></slot>
  </div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    icon: {
      type: String,
      default: 'icon-folder-open'
    },
    title: {
      type: String,
      default: 'No Data'
    },
    description: {
      type: String,
      default: 'There is nothing to display here'
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    }
  }
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

.empty-state.small {
  padding: 20px;
}

.empty-state.medium {
  padding: 40px 20px;
}

.empty-state.large {
  padding: 60px 20px;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-state.small .empty-icon {
  font-size: 32px;
}

.empty-state.medium .empty-icon {
  font-size: 48px;
}

.empty-state.large .empty-icon {
  font-size: 64px;
}

.empty-icon i {
  display: block;
}

.empty-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #6b7280;
}

.empty-state.small .empty-title {
  font-size: 14px;
}

.empty-state.medium .empty-title {
  font-size: 18px;
}

.empty-state.large .empty-title {
  font-size: 22px;
}

.empty-description {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
  max-width: 400px;
}

.empty-state.small .empty-description {
  font-size: 12px;
}

.empty-state.medium .empty-description {
  font-size: 14px;
}

.empty-state.large .empty-description {
  font-size: 16px;
}
</style>
