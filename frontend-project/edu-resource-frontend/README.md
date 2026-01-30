# Edu Resource Platform - Frontend

## Project Information
- **Project Name**: edu-resource-frontend
- **Framework**: Vue 3
- **Build Tool**: Vue CLI 5.0.9
- **Package Manager**: npm

## Technology Stack
- **Vue 3**: Progressive JavaScript framework
- **Vue Router 4**: Official router for Vue.js
- **Vuex 4**: State management pattern + library
- **Less**: CSS pre-processor
- **Babel**: JavaScript compiler

## Prerequisites
- Node.js 14.x or higher
- npm 6.x or higher
- Vue CLI 5.0.9 or higher

## Installation

### Step 1: Navigate to Project Directory
```bash
cd C:\APP\Graduation-Design-EduResourcePlatform\frontend-project\edu-resource-frontend
```

### Step 2: Install Dependencies
```bash
npm install
```

## Development

### Start Development Server
```bash
npm run serve
```

The application will be available at: **http://localhost:8080/**

### Build for Production
```bash
npm run build
```

The production build will be in the `dist` directory.

### Lint Code
```bash
npm run lint
```

## Project Structure
```
edu-resource-frontend/
├── public/
│   ├── favicon.ico
│   └── index.html
├── src/
│   ├── assets/
│   │   └── logo.png
│   ├── components/
│   │   └── HelloWorld.vue
│   ├── App.vue
│   └── main.js
├── .gitignore
├── babel.config.js
├── jsconfig.json
├── package.json
├── package-lock.json
└── vue.config.js
```

## Configuration

### Vue Router
To add Vue Router, create `src/router/index.js`:
```javascript
import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
```

### Vuex Store
To add Vuex, create `src/store/index.js`:
```javascript
import { createStore } from 'vuex'

export default createStore({
  state: {
  },
  mutations: {
  },
  actions: {
  },
  modules: {
  }
})
```

### Less Configuration
Less is already configured. You can use `.less` files in your components:
```vue
<style lang="less">
.container {
  .header {
    color: #333;
  }
}
</style>
```

## API Integration

### Example API Call
```javascript
import axios from 'axios'

export default {
  methods: {
    async fetchResources() {
      try {
        const response = await axios.get('http://localhost:8080/edu-resource-backend/api/resources')
        this.resources = response.data.data
      } catch (error) {
        console.error('Error fetching resources:', error)
      }
    }
  }
}
```

## Troubleshooting

### Issue: "Running completion hooks..." hangs
**Solution**: 
1. Press `Ctrl + C` to stop the process
2. Delete the `node_modules` folder and `package-lock.json`
3. Run `npm cache clean --force`
4. Run `npm install` again
5. Run `npm run serve`

### Issue: Port 8080 already in use
**Solution**: Modify the port in `vue.config.js`:
```javascript
module.exports = {
  devServer: {
    port: 8081
  }
}
```

### Issue: Module not found errors
**Solution**: 
1. Run `npm install` to install all dependencies
2. Check if the module is listed in `package.json`
3. Delete `node_modules` and run `npm install` again

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run serve` | Start development server |
| `npm run build` | Build for production |
| `npm run lint` | Run ESLint |

## Browser Support
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Development Notes
- All file names, class names, and variable names are in English
- The project uses Vue 3 Composition API
- Component files use `.vue` extension
- Styles can be written in CSS, SCSS, or Less

## Next Steps
1. Set up Vue Router for navigation
2. Configure Vuex for state management
3. Create reusable components
4. Integrate with backend API
5. Add authentication and authorization
6. Implement resource management features

## Useful Resources
- [Vue 3 Documentation](https://v3.vuejs.org/)
- [Vue Router Documentation](https://router.vuejs.org/)
- [Vuex Documentation](https://vuex.vuejs.org/)
- [Vue CLI Documentation](https://cli.vuejs.org/)
