<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <div class="error-code">404</div>
      <h1>Page Not Found</h1>
      <p>The page you are looking for does not exist or has been moved.</p>
      <router-link to="/" class="btn btn-primary btn-cta">
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
  background: transparent;
  padding: 24px;
}

.not-found-container {
  text-align: center;
  color: var(--text);
  max-width: 500px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--border);
  border-radius: 22px;
  box-shadow: var(--shadow-lg);
  padding: 28px 22px;
  backdrop-filter: blur(14px);
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 20px;
  letter-spacing: -0.06em;
  background: linear-gradient(135deg, rgba(79, 109, 255, 0.96) 0%, rgba(123, 92, 255, 0.96) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.not-found-container h1 {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.not-found-container p {
  font-size: 16px;
  margin: 0 0 32px 0;
  color: var(--text-2);
  line-height: 1.6;
}
</style>
