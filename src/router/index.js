import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '../views/Layout.vue'

const routes = [
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

export default router
