import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '../views/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    redirect: '/dashboard',
    component: Layout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
      },
      {
        path: 'templates',
        name: 'TemplateList',
        component: () => import('../views/TemplateList.vue'),
      },
      {
        path: 'templates/new',
        name: 'TemplateCreate',
        component: () => import('../views/TemplateEdit.vue'),
      },
      {
        path: 'templates/:id/edit',
        name: 'TemplateEdit',
        component: () => import('../views/TemplateEdit.vue'),
      },
      {
        path: 'pois',
        name: 'PoiList',
        component: () => import('../views/PoiList.vue'),
      },
      {
        path: 'pois/new',
        name: 'PoiCreate',
        component: () => import('../views/PoiEdit.vue'),
      },
      {
        path: 'pois/:id/edit',
        name: 'PoiEdit',
        component: () => import('../views/PoiEdit.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else {
    next()
  }
})

export default router
