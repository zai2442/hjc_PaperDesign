<template>
  <div class="student-activity-list">
    <el-tabs v-model="filters.statusCategory" @tab-click="onTabClick" class="status-tabs">
      <el-tab-pane label="全部活动" name="" />
      <el-tab-pane label="报名中" name="ENROLLING" />
      <el-tab-pane label="进行中" name="IN_PROGRESS" />
      <el-tab-pane label="已结束" name="ENDED" />
    </el-tabs>

    <!-- Filters -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" placeholder="活动标题/简介" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部分类" clearable>
            <el-option label="学术讲座" value="学术讲座" />
            <el-option label="社团活动" value="社团活动" />
            <el-option label="体育赛事" value="体育赛事" />
            <el-option label="文艺演出" value="文艺演出" />
            <el-option label="志愿服务" value="志愿服务" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DDTHH:mm:ss"
            clearable
          />
        </el-form-item>
        <el-form-item label="排序方式">
          <el-select v-model="filters.sortBy" @change="handleSearch" style="width: 120px;">
            <el-option label="最新发布" value="LATEST" />
            <el-option label="最热活动" value="HOTTEST" />
            <el-option label="即将开始" value="UPCOMING" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="filters.hasSpots" @change="handleSearch">仅看有名额</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="success" @click="$router.push('/checkin/scan')">扫码签到</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Activity Grid -->
    <div class="activity-grid">
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in activityList" :key="item.id" style="margin-bottom: 20px;">
          <el-card class="activity-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <h3 class="clickable-title" @click="goToDetail(item.id)">{{ item.title }}</h3>
                <el-tag :type="item.stockAvailable > 0 ? 'success' : 'danger'">
                  {{ item.stockAvailable > 0 ? '剩余 ' + item.stockAvailable : '名额已满' }}
                </el-tag>
              </div>
            </template>
            <div class="card-body">
              <p class="summary">{{ item.summary || '暂无简介' }}</p>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>时间: {{ formatDate(item.startTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>地点: {{ item.location || '待定' }}</span>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>名额: {{ item.stockTotal - item.stockAvailable }}/{{ item.stockTotal }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-button type="primary" style="width: 100%" @click="openRegisterDialog(item)" :disabled="item.stockAvailable <= 0">
                立即报名
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="activityList.length === 0" description="暂无符合条件的活动" />
    </div>

    <!-- Pagination -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <!-- Register Dialog -->
    <el-dialog v-model="registerDialogVisible" title="确认报名" width="400px">
      <p>准备报名参与：<strong>{{ currentActivity?.title }}</strong></p>
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
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Location, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const filters = reactive({
  keyword: '',
  type: '',
  hasSpots: false,
  statusCategory: '',
  sortBy: 'LATEST'
})
const dateRange = ref([])
const page = ref(1)
const size = ref(12)
const total = ref(0)
const activityList = ref([])

const router = useRouter()

const loadData = async () => {
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      hasSpots: filters.hasSpots,
      statusCategory: filters.statusCategory || undefined,
      sortBy: filters.sortBy || undefined
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res = await request.get('/activities', { params })
    activityList.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    ElMessage.error('加载活动列表失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const onTabClick = (tab) => {
  filters.statusCategory = tab.props.name
  page.value = 1
  loadData()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.type = ''
  filters.hasSpots = false
  filters.statusCategory = ''
  filters.sortBy = 'LATEST'
  dateRange.value = []
  page.value = 1
  loadData()
}

const goToDetail = (id) => {
  router.push(`/activity/detail/${id}`)
}

const registerDialogVisible = ref(false)
const currentActivity = ref(null)
const extraData = ref('')
const registering = ref(false)

const openRegisterDialog = (activity) => {
  currentActivity.value = activity
  extraData.value = ''
  registerDialogVisible.value = true
}

const submitRegistration = async () => {
  try {
    registering.value = true
    await request.post('/registrations', {
      activityId: currentActivity.value.id,
      extraData: extraData.value
    })
    ElMessage.success('报名成功！您可在"我的报名"中查看状态。')
    registerDialogVisible.value = false
    loadData() // Refresh remaining spots
  } catch (error) {
    ElMessageBox.alert(error.response?.data?.message || '报名失败，名额已满、已在线外、限流或重复报名', '报名失败', { type: 'error' })
  } finally {
    registering.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '待定'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.student-activity-list {
  padding: 20px;
}
.filter-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 10px;
}
.clickable-title {
  margin: 0;
  font-size: 16px;
  cursor: pointer;
  color: #303133;
  transition: color 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 10px;
}
.clickable-title:hover {
  color: #409EFF;
}
.card-body {
  height: 120px;
  display: flex;
  flex-direction: column;
}
.summary {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.info-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #999;
  margin-bottom: 5px;
}
.info-item .el-icon {
  margin-right: 5px;
}
.info-item span {
  font-size: 14px;
}
.card-footer {
  margin-top: 15px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.status-tabs {
  margin-bottom: 10px;
  background: white;
  padding: 0 20px;
  border-radius: 4px;
}
</style>
```
