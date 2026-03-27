<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>Campus Activity Management</h2>
      <el-form :model="loginForm" label-width="0">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="Username" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="Password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">Login</el-button>
        </el-form-item>
      </el-form>
      <div class="register-link">
        <span>Don't have an account? </span>
        <el-link type="primary" @click="$router.push('/register')">Register Now</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  try {
    const res = await request.post('/auth/login', loginForm)
    localStorage.setItem('token', res.data)
    
    // Fetch user info to get roles and username
    const userRes = await request.get('/users/me')
    localStorage.setItem('username', userRes.data.username)
    if (userRes.data.roles && userRes.data.roles.length > 0) {
      localStorage.setItem('user_role', userRes.data.roles[0].roleCode)
    }
    
    ElMessage.success('Login Successful')
    const redirectPath = route.query.redirect
    if (redirectPath) {
      router.push(redirectPath)
    } else if (userRes.data.roles && userRes.data.roles.some(r => r.roleCode === 'ROLE_STUDENT')) {
      router.push('/student/activities')
    } else {
      router.push('/user-management')
    }
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.login-card {
  width: 400px;
  padding: 20px;
  text-align: center;
}
.register-link {
  margin-top: 15px;
  font-size: 14px;
}
</style>
