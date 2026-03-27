import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: to => {
      const role = localStorage.getItem('user_role')
      if (role === 'ROLE_STUDENT') return '/student/activities'
      return '/user-management'
    },
    children: [
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('../views/user/UserManagement.vue')
      },
      {
        path: 'activity/list',
        name: 'ActivityList',
        component: () => import('../views/activity/ActivityList.vue')
      },
      {
        path: 'student/activities',
        name: 'StudentActivityList',
        component: () => import('../views/activity/StudentActivityList.vue')
      },
      {
        path: 'activity/detail/:id',
        name: 'ActivityDetail',
        component: () => import('../views/activity/ActivityDetail.vue')
      },
      {
        path: 'activity/create',
        name: 'ActivityCreate',
        component: () => import('../views/activity/ActivityEdit.vue')
      },
      {
        path: 'activity/edit/:id',
        name: 'ActivityEdit',
        component: () => import('../views/activity/ActivityEdit.vue')
      },
      {
        path: 'registration/my',
        name: 'MyRegistrations',
        component: () => import('../views/registration/MyRegistrations.vue')
      },
      {
        path: 'registration/admin',
        name: 'AdminRegistrations',
        component: () => import('../views/registration/AdminRegistrations.vue')
      },
      {
        path: 'checkin/admin',
        name: 'CheckInAdmin',
        component: () => import('../views/activity/CheckInAdmin.vue')
      },
      {
        path: 'personal-center',
        name: 'PersonalCenter',
        component: () => import('../views/user/PersonalCenter.vue')
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/checkin/scan',
    name: 'CheckInScan',
    component: () => import('../views/activity/CheckInScan.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Global navigation guard for authentication & role checks
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('user_role')
  // Allow unauthenticated access to login and register pages
  if (!token && !['/login', '/register', '/checkin/scan'].includes(to.path)) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  
  if (!token && to.path === '/checkin/scan') {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  // Allow students or unknown roles (will be fetched in Layout) to access student routes initially
  if (to.path.startsWith('/student') && role && role !== 'ROLE_STUDENT') {
    return next('/')
  }
  next()
})

export default router
