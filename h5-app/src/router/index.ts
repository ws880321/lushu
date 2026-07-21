import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/',
    redirect: '/home',
    component: () => import('../views/TabLayout.vue'),
    children: [
      { path: 'home', name: 'Home', component: () => import('../views/HomeView.vue'), meta: { tab: 0 } },
      { path: 'generate', name: 'Generate', component: () => import('../views/GenerateView.vue'), meta: { tab: 1 } },
      { path: 'record', name: 'Record', component: () => import('../views/RecordView.vue'), meta: { tab: 2 } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/DashboardView.vue'), meta: { tab: 3 } },
      { path: 'community', name: 'Community', component: () => import('../views/CommunityView.vue'), meta: { tab: 4 } },
      { path: 'profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { tab: 5 } },
    ],
  },
  { path: '/route/:id', name: 'RouteDetail', component: () => import('../views/RouteDetailView.vue') },
  { path: '/compare', name: 'Compare', component: () => import('../views/CompareView.vue') },
  { path: '/share/:id', name: 'Share', component: () => import('../views/ShareView.vue'), meta: { noAuth: true } },
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to, _from, next) => {
  if (!localStorage.getItem('app_token') && !to.meta.noAuth) next('/login')
  else next()
})

router.beforeResolve((to, _from, next) => {
  if (to.name === 'RouteDetail' && (to.params as any).id) {
    import('../views/RouteDetailView.vue')
  }
  next()
})

export default router
