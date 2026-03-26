<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>Register</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" label-position="left">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Username" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" placeholder="Password" />
        </el-form-item>
        <el-form-item label="Confirm" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="Confirm Password" />
        </el-form-item>
        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" placeholder="example@domain.com" />
        </el-form-item>
        <el-form-item label="Phone" prop="phone">
          <el-input v-model="form.phone" placeholder="Phone Number" />
        </el-form-item>
        <el-form-item label-width="0">
          <el-button type="primary" @click="handleRegister" style="width: 100%" :loading="loading">Register</el-button>
        </el-form-item>
      </el-form>
      <div class="login-link">
        <span>Already have an account? </span>
        <el-link type="primary" @click="$router.push('/login')">Log In</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('Please confirm the password'))
  } else if (value !== form.value.password) {
    callback(new Error('Passwords do not match!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: 'Please input username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please input password', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
  email: [
    { required: true, message: 'Please input email address', trigger: 'blur' },
    { type: 'email', message: 'Please input correct email address', trigger: ['blur', 'change'] }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.post('/auth/register', {
          username: form.value.username,
          password: form.value.password,
          email: form.value.email,
          phone: form.value.phone
        })
        ElMessage.success('Registration Successful! You can now log in.')
        router.push('/login')
      } catch (error) {
        // Errors are usually caught and displayed by request interceptors.
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.register-card {
  width: 450px;
  padding: 30px;
}
.register-card h2 {
  text-align: center;
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
}
.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}
</style>
