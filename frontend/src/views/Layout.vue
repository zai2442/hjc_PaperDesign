<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        router
      >
        <el-menu-item index="/student/activities" v-if="userRole === 'ROLE_STUDENT'">
          <el-icon><Calendar /></el-icon>
          <span>活动大厅</span>
        </el-menu-item>
        <el-menu-item index="/registration/my" v-if="userRole === 'ROLE_STUDENT'">
          <el-icon><Tickets /></el-icon>
          <span>我的报名</span>
        </el-menu-item>

        <el-menu-item index="/user-management" v-if="userRole === 'ROLE_SUPER_ADMIN'">
          <el-icon><User /></el-icon>
          <span>用户角色管理</span>
        </el-menu-item>
        <el-menu-item index="/activity/list" v-if="userRole !== 'ROLE_STUDENT'">
          <el-icon><Calendar /></el-icon>
          <span>活动管理</span>
        </el-menu-item>
        <el-menu-item index="/registration/admin" v-if="canManageRegistrations">
          <el-icon><List /></el-icon>
          <span>报名管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          校园活动管理系统
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ username }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Calendar, ArrowDown, Tickets, List } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const username = ref(localStorage.getItem('username') || '用户')
const userRole = computed(() => localStorage.getItem('user_role') || 'ROLE_STUDENT')
const canManageRegistrations = computed(() => ['ROLE_SUPER_ADMIN', 'ROLE_COUNSELOR', 'ROLE_CLUB_OWNER'].includes(userRole.value))

const fetchUserInfo = async () => {
  if (localStorage.getItem('token') && !localStorage.getItem('user_role')) {
    try {
      const res = await request.get('/users/me')
      username.value = res.data.username
      localStorage.setItem('username', res.data.username)
      if (res.data.roles && res.data.roles.length > 0) {
        localStorage.setItem('user_role', res.data.roles[0].roleCode)
      }
      window.location.reload()
    } catch (err) {
      console.error('Failed to fetch user info', err)
    }
  }
}

onMounted(() => {
  fetchUserInfo()
})

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('user_role')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-menu-vertical {
  height: 100%;
  border-right: none;
}
.el-aside {
  background-color: #304156;
  color: #fff;
}
.el-menu {
  background-color: #304156;
  border-right: none;
}
:deep(.el-menu-item) {
  color: #bfcbd9;
}
:deep(.el-menu-item.is-active) {
  color: #409eff;
  background-color: #263445;
}
:deep(.el-menu-item:hover) {
  background-color: #263445;
}
.layout-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.header-left {
  font-size: 18px;
  font-weight: bold;
}
.el-dropdown-link {
  cursor: pointer;
  color: #409eff;
  display: flex;
  align-items: center;
}
</style>
