<template>
  <div class="checkin-scan-container">
    <el-card class="scan-result-card">
      <div v-if="loading" class="state-loading">
        <el-icon class="is-loading" size="60" color="#409EFF"><Loading /></el-icon>
        <p>正在签到中，请稍候...</p>
      </div>

      <div v-else-if="success" class="state-success">
        <el-icon size="80" color="#67C23A"><CircleCheckFilled /></el-icon>
        <h2 style="color: #67C23A; margin: 20px 0;">签到成功！</h2>
        <p>您已成功签到，活动即将开始。</p>
        <el-button type="primary" @click="goHome" style="margin-top: 30px;">返回首页</el-button>
      </div>

      <!-- Manual Input State (for testing without QR) -->
      <div v-else-if="manualInput" class="state-manual">
        <el-icon size="60" color="#909399" style="margin-bottom: 20px"><Crop /></el-icon>
        <h3>模拟扫码签到</h3>
        <p style="font-size: 14px; color: #666; margin-bottom: 20px;">
          在电脑端测试，请输入活动ID和签到码。
        </p>
        <el-form label-width="80px">
          <el-form-item label="活动 ID">
            <el-input v-model="manualForm.activityId" placeholder="Activity ID" />
          </el-form-item>
          <el-form-item label="签到码">
            <el-input v-model="manualForm.token" placeholder="Token string" />
          </el-form-item>
          <el-button type="primary" @click="submitManualCheckIn" style="width: 100%; margin-top: 10px;">
            模拟签到
          </el-button>
        </el-form>
      </div>

      <div v-else class="state-error">
        <el-icon size="80" color="#F56C6C"><CircleCloseFilled /></el-icon>
        <h2 style="color: #F56C6C; margin: 20px 0;">签到失败</h2>
        <p>{{ errorMessage }}</p>
        <el-button @click="retryScan" style="margin-top: 30px;">重新扫码</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { scanCheckInQrcode } from '../../api/checkin'
import { Loading, CircleCheckFilled, CircleCloseFilled, Crop } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const success = ref(false)
const manualInput = ref(false)
const errorMessage = ref('')

const activityId = route.query.activityId
const token = route.query.token

const manualForm = reactive({
  activityId: '',
  token: ''
})

const performCheckIn = async (aid, tkn) => {
  if (!aid || !tkn) {
    loading.value = false
    success.value = false
    manualInput.value = true // switch to manual input form
    return
  }
  
  try {
    loading.value = true
    manualInput.value = false
    await scanCheckInQrcode(aid, tkn)
    success.value = true
  } catch (err) {
    success.value = false
    errorMessage.value = err.response?.data?.message || err.message || '二维码已过期、失效或您未报名通过'
  } finally {
    loading.value = false
  }
}

const submitManualCheckIn = () => {
  if (!manualForm.activityId || !manualForm.token) {
    errorMessage.value = '请输入活动ID和签到码'
    return
  }
  performCheckIn(manualForm.activityId, manualForm.token)
}

const goHome = () => {
  router.push('/')
}

const retryScan = () => {
  // If they were manually inputting, they can go back to it
  if (!route.query.activityId || !route.query.token) {
    manualInput.value = true
    errorMessage.value = ''
    return
  }
  errorMessage.value = '请要求组织者出示最新的二维码为您签到'
}

onMounted(() => {
  performCheckIn(activityId, token)
})
</script>

<style scoped>
.checkin-scan-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.scan-result-card {
  width: 90%;
  max-width: 400px;
  text-align: center;
  padding: 40px 20px;
  border-radius: 12px;
}
.state-loading, .state-success, .state-error {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
