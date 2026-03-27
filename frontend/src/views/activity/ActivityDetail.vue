<template>
  <div class="activity-detail-container">
    <el-page-header @back="goBack" content="活动详情" style="margin-bottom: 20px" />
    
    <div v-if="loading" style="text-align: center; padding: 50px;">
      <el-icon class="is-loading" size="40"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <el-row :gutter="20" v-else-if="activity">
      <el-col :span="16">
        <el-card shadow="hover" class="main-card">
          <h1 class="activity-title">{{ activity.title }}</h1>
          <div class="activity-meta">
            <el-tag style="margin-right: 10px">{{ activity.contentType }}</el-tag>
            <span class="meta-item"><el-icon><Calendar /></el-icon> 发布时间: {{ formatDate(activity.publishAt) }}</span>
            <span class="meta-item"><el-icon><View /></el-icon> 浏览量: {{ activity.stockTotal - activity.stockAvailable }} 人已关注/报名</span>
          </div>
          <el-divider />
          <div class="activity-content" v-html="activity.content || activity.summary || '暂无内容详述'"></div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="info-card">
          <template #header>
            <div class="card-header">
              <span>基础信息</span>
            </div>
          </template>
          
          <div class="info-list">
            <div class="info-item">
              <label><el-icon><Calendar /></el-icon> 活动时间</label>
              <div>{{ formatDate(activity.startTime) }} ~ {{ formatDate(activity.endTime) }}</div>
            </div>
            <div class="info-item" v-if="activity.regStartTime">
              <label><el-icon><Timer /></el-icon> 报名时间</label>
              <div>{{ formatDate(activity.regStartTime) }} ~ {{ formatDate(activity.regEndTime) }}</div>
            </div>
            <div class="info-item">
              <label><el-icon><Location /></el-icon> 活动地点</label>
              <div>{{ activity.location || '待定' }}</div>
            </div>
            <div class="info-item">
              <label><el-icon><User /></el-icon> 招募名额</label>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>{{ activity.stockTotal - activity.stockAvailable }} / {{ activity.stockTotal }} 人</span>
                <el-tag :type="activity.stockAvailable > 0 ? 'success' : 'danger'">
                  {{ activity.stockAvailable > 0 ? '剩余 ' + activity.stockAvailable : '名额已满' }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <div class="action-box">
             <el-button 
               type="primary" 
               size="large" 
               style="width: 100%" 
               @click="openRegisterDialog" 
               :disabled="activity.stockAvailable <= 0"
             >
               立即报名
             </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Register Dialog -->
    <el-dialog v-model="registerDialogVisible" title="确认报名" width="400px">
      <p>准备报名参与：<strong>{{ activity?.title }}</strong></p>
      <el-form>
        <el-form-item label="备注留言">
          <el-input v-model="extraData" type="textarea" placeholder="有什么想对组织者说的（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegistration" :loading="registering">确认报名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Loading, Calendar, View, Location, User, Timer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const activityId = route.params.id

const loading = ref(true)
const activity = ref(null)

const registerDialogVisible = ref(false)
const extraData = ref('')
const registering = ref(false)

const goBack = () => {
  router.back()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '待定'
  return dateStr.replace('T', ' ').substring(0, 16)
}

const loadDetail = async () => {
  try {
    loading.value = true
    const res = await request.get(`/activities/${activityId}`)
    activity.value = res.data
  } catch (error) {
    ElMessage.error('获取活动详情失败')
  } finally {
    loading.value = false
  }
}

const openRegisterDialog = () => {
  extraData.value = ''
  registerDialogVisible.value = true
}

const submitRegistration = async () => {
  registering.value = true
  try {
    await request.post('/registrations', {
      activityId: activity.value.id,
      extraData: extraData.value ? JSON.stringify({ note: extraData.value }) : null
    })
    ElMessage.success('报名已提交，请等待审核')
    registerDialogVisible.value = false
    loadDetail() // Refresh stock info
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '报名失败，可能已报满或重复报名')
  } finally {
    registering.value = false
  }
}

onMounted(() => {
  if (activityId) {
    loadDetail()
  } else {
    ElMessage.error('无效的活动ID')
    router.push('/student/activities')
  }
})
</script>

<style scoped>
.activity-detail-container {
  padding: 20px;
}
.main-card {
  min-height: 500px;
}
.activity-title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 24px;
  color: #303133;
}
.activity-meta {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 13px;
}
.meta-item {
  display: flex;
  align-items: center;
  margin-right: 20px;
}
.meta-item .el-icon {
  margin-right: 4px;
}
.activity-content {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}
.info-card {
  position: sticky;
  top: 20px;
}
.info-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.info-item label {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.info-item label .el-icon {
  margin-right: 5px;
}
.info-item div {
  font-size: 14px;
  color: #303133;
}
.action-box {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
