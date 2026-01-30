<template>
  <div id="app">
    <NavBar />
    <main class="main-content">
      <router-view />
    </main>
    <ToastContainer ref="toastContainer" />
  </div>
</template>

<script>
import NavBar from './components/NavBar.vue'
import ToastContainer from './components/ToastContainer.vue'
import request from './utils/request'

export default {
  name: 'App',
  components: {
    NavBar,
    ToastContainer
  },
  mounted() {
    this.$store.dispatch('loadUserFromStorage')
  }
}
</script>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1f2937;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding-top: 64px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: inherit;
  line-height: 1.6;
  color: #1f2937;
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  font-family: inherit;
}

input,
textarea,
select {
  font-family: inherit;
}
</style>
