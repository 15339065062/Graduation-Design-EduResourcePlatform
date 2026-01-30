<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <div class="error-code">404</div>
      <h1>Page Not Found</h1>
      <p>The page you are looking for does not exist or has been moved.</p>
      <router-link to="/" class="btn btn-primary">
        <i class="icon-home"></i> Back to Home
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound'
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.not-found-container {
  text-align: center;
  color: white;
  max-width: 500px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.not-found-container h1 {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.not-found-container p {
  font-size: 16px;
  margin: 0 0 32px 0;
  opacity: 0.9;
  line-height: 1.6;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-primary {
  background: white;
  color: #667eea;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
</style>
