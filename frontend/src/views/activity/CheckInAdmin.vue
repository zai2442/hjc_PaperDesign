<template>
  <div class="checkin-admin-container">
    <el-page-header @back="goBack" content="签到管理" style="margin-bottom: 20px" />
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>动态二维码签到</span>
              <el-switch
                v-model="isCheckingIn"
                active-text="开启签到"
                inactive-text="关闭签到"
                @change="toggleCheckIn"
              />
            </div>
          </template>
          
          <div v-if="isCheckingIn" class="qrcode-wrapper">
            <qrcode-vue :value="scanUrl" :size="250" level="H" />
            <p class="refresh-tips">页面二维码每 30 秒自动刷新，防止截图</p>
            <p class="url-tips">扫描上方二维码或访问：<br/> <a :href="scanUrl" target="_blank">{{ scanUrl }}</a></p>
          </div>
          <div v-else class="qrcode-wrapper empty-state">
            <el-empty description="签到尚未开始，请开启签到开关" />
          </div>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>签到实时统计</span>
              <el-button link type="primary" @click="fetchStats">刷新</el-button>
            </div>
          </template>
          <div class="stats-data">
            <h2>{{ stats.checkedIn }} / {{ stats.totalRegistered }}</h2>
            <p>已签到人数 / 总报名人数</p>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>报名与签到名单 (仅显示已通过审核人员)</span>
              <el-button link type="primary" @click="fetchRegistrations">刷新名单</el-button>
            </div>
          </template>
          
          <el-table :data="mergedList" v-loading="loading" style="width: 100%" height="500">
            <el-table-column prop="userId" label="用户 ID" width="100" />
            <el-table-column prop="status" label="签到状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.checkedIn ? 'success' : 'info'">
                  {{ row.checkedIn ? '已签到' : '未签到' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="签到方式" width="120">
              <template #default="{ row }">
                 <span v-if="row.checkedIn">{{ row.type === 'SCAN' ? '扫码' : '手动' }}</span>
                 <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="checkInTime" label="签到时间" min-width="160">
               <template #default="{ row }">
                 {{ row.checkInTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button 
                   type="primary" 
                   size="small" 
                   v-if="!row.checkedIn"
                   @click="handleManualCheckIn(row.userId)"
                >
                  补签
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QrcodeVue from 'qrcode.vue'
import { 
  startCheckIn, 
  stopCheckIn, 
  getCheckInQrcode, 
  getCheckInStats, 
  manualCheckIn 
} from '../../api/checkin'
import { getAdminRegistrations } from '../../api/registration' // Need to fetch all approved users

const route = useRoute()
const router = useRouter()
const activityId = route.query.activityId

const isCheckingIn = ref(false)
const currentToken = ref('')
const scanUrl = computed(() => {
  const base = window.location.origin
  return `${base}/checkin/scan?activityId=${activityId}&token=${currentToken.value}`
})

const stats = ref({ checkedIn: 0, totalRegistered: 0, records: [] })
const registrations = ref([])
const loading = ref(false)

let qrcodeTimer = null
let statsTimer = null

const goBack = () => {
  router.push('/activity/list')
}

const toggleCheckIn = async (val) => {
  try {
    if (val) {
      await startCheckIn(activityId)
      ElMessage.success('签到已开启')
      fetchQrcode()
      startTimers()
    } else {
      await stopCheckIn(activityId)
      ElMessage.success('签到已关闭')
      stopTimers()
      currentToken.value = ''
    }
  } catch (error) {
    isCheckingIn.value = !val // revert
    ElMessage.error(val ? '开启失败' : '关闭失败')
  }
}

const fetchQrcode = async () => {
  if (!isCheckingIn.value) return
  try {
    const res = await getCheckInQrcode(activityId)
    currentToken.value = res.data.token
  } catch (err) {
    if (err.response?.status === 400 && err.response?.data?.message === 'Check-in not started') {
        isCheckingIn.value = false
        stopTimers()
    }
  }
}

const fetchStats = async () => {
  try {
    const res = await getCheckInStats(activityId)
    stats.value = res.data
  } catch (error) {
    console.error('Failed to fetch checkin stats', error)
  }
}

const fetchRegistrations = async () => {
  try {
    loading.value = true
    const res = await getAdminRegistrations({
      activityId,
      status: 'APPROVED',
      page: 1,
      size: 1000 // Simplified pagination for MVP
    })
    registrations.value = res.data.records
  } catch (error) {
    ElMessage.error('获取报名名单失败')
  } finally {
    loading.value = false
  }
}

const handleManualCheckIn = async (userId) => {
  ElMessageBox.confirm('确认手动为该用户补签吗？', '提示').then(async () => {
    try {
      await manualCheckIn(activityId, userId)
      ElMessage.success('补签成功')
      fetchStats()
    } catch (error) {
      ElMessage.error(error.message || '补签失败')
    }
  })
}

// Merge registered users with check-in records
const mergedList = computed(() => {
  const checkinMap = {}
  stats.value.records.forEach(r => checkinMap[r.userId] = r)

  return registrations.value.map(reg => {
    const record = checkinMap[reg.userId]
    return {
      userId: reg.userId,
      checkedIn: !!record,
      type: record ? record.type : null,
      checkInTime: record ? record.checkInTime.replace('T', ' ') : null
    }
  })
})

const startTimers = () => {
  if (!qrcodeTimer) {
     qrcodeTimer = setInterval(fetchQrcode, 25000) // refresh every 25s
  }
  if (!statsTimer) {
     statsTimer = setInterval(fetchStats, 5000) // refresh stats every 5s
  }
}

const stopTimers = () => {
  if (qrcodeTimer) clearInterval(qrcodeTimer)
  if (statsTimer) clearInterval(statsTimer)
  qrcodeTimer = null
  statsTimer = null
}

const initLoad = async () => {
  // Check if session is already active by trying to fetch qrcode
  try {
    const res = await getCheckInQrcode(activityId)
    if (res && res.data && res.data.token) {
      currentToken.value = res.data.token
      isCheckingIn.value = true
    } else {
      isCheckingIn.value = false
    }
  } catch (e) {
    isCheckingIn.value = false
  }
  
  fetchRegistrations()
  fetchStats()
  
  if (isCheckingIn.value) {
    startTimers()
  }
}

onMounted(() => {
  if (!activityId) {
    ElMessage.error('缺失活动ID')
    router.push('/activity/list')
    return
  }
  initLoad()
})

onUnmounted(() => {
  stopTimers()
})
</script>

<style scoped>
.checkin-admin-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.qrcode-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}
.empty-state {
  height: 250px;
  justify-content: center;
}
.refresh-tips {
  margin-top: 20px;
  color: #E6A23C;
  font-size: 14px;
}
.url-tips {
  margin-top: 10px;
  color: #999;
  font-size: 12px;
  text-align: center;
  word-break: break-all;
}
.stats-data {
  text-align: center;
  padding: 20px 0;
}
.stats-data h2 {
  font-size: 36px;
  color: #409EFF;
  margin: 0 0 10px 0;
}
.stats-data p {
  color: #666;
  margin: 0;
}
</style>
