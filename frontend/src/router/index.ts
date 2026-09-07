import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import AccountsView from '@/views/AccountsView.vue'
import PaymentsView from '@/views/PaymentsView.vue'
import CustomersView from '@/views/CustomersView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    { path: '/accounts', name: 'accounts', component: AccountsView },
    { path: '/payments', name: 'payments', component: PaymentsView },
    { path: '/customers', name: 'customers', component: CustomersView, meta: { adminOnly: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.meta.public && auth.isAuthenticated) {
    return { name: 'home' }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router
