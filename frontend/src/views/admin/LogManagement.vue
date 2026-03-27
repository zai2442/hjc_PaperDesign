<template>
  <div class="log-container">
    <div class="header">
      <h2 class="title">系统操作日志</h2>
      <el-alert
        v-if="newLogAvailable"
        title="有新日志产生"
        type="info"
        description="点击按钮刷新列表并查看最新日志"
        show-icon
        @close="newLogAvailable = false"
      >
        <template #default>
          <el-button size="small" type="primary" plain @click="refreshWithNewLog">立即刷新</el-button>
        </template>
      </el-alert>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryForm" class="demo-form-inline">
        <el-form-item label="操作类型">
          <el-select
            v-model="queryForm.opTypes"
            multiple
            placeholder="请选择"
            style="width: 240px"
            @change="handleSearch"
          >
            <el-option label="活动下线" value="OFFLINE" />
            <el-option label="白名单添加" value="WHITELIST_ADD" />
            <el-option label="白名单删除" value="WHITELIST_REMOVE" />
            <el-option label="删除记录" value="DELETE" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :shortcuts="shortcuts"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleTimeRangeChange"
          />
        </el-form-item>

        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            placeholder="ID/名称/账号/昵称"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="logs"
        style="width: 100%"
        @sort-change="handleSortChange"
        border
        stripe
      >
        <el-table-column prop="createdAt" label="操作时间" width="180" sortable="custom" />
        <el-table-column label="操作人" width="180" sortable="custom" prop="operatorUsername">
          <template #default="{ row }">
            {{ row.operatorNickname }} ({{ row.operatorUsername }})
          </template>
        </el-table-column>
        <el-table-column prop="activityId" label="活动 ID" width="100" sortable="custom" />
        <el-table-column prop="activityTitle" label="活动名称" min-width="150" sortable="custom" show-overflow-tooltip />
        <el-table-column prop="opType" label="操作类型" width="120" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="getOpTypeTag(row.opType)">{{ getOpTypeLabel(row.opType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opResult" label="结果" width="100" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.opResult === 1 ? 'success' : 'danger'">
              {{ row.opResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-drawer
      v-model="drawer.visible"
      title="操作详情"
      size="50%"
      destroy-on-close
    >
      <div v-if="drawer.data" class="drawer-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作 ID">{{ drawer.data.id }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ drawer.data.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ drawer.data.operatorNickname }} ({{ drawer.data.operatorUsername }})</el-descriptions-item>
          <el-descriptions-item label="相关活动">{{ drawer.data.activityTitle }} (ID: {{ drawer.data.activityId }})</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ getOpTypeLabel(drawer.data.opType) }}</el-descriptions-item>
          <el-descriptions-item label="操作结果">
            <el-tag :type="drawer.data.opResult === 1 ? 'success' : 'danger'">
              {{ drawer.data.opResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="drawer.data.errorMsg" label="错误信息">
            <span class="error-text">{{ drawer.data.errorMsg }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="json-detail">
          <div class="json-header">
            <span>完整 JSON 详情</span>
            <el-button size="small" @click="copyJson">一键复制</el-button>
          </div>
          <pre class="json-pre">{{ formattedJson }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getLogs } from '../../api/log'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const logs = ref([])
const total = ref(0)
const timeRange = ref([])
const newLogAvailable = ref(false)
let ws = null

const queryForm = ref({
  page: 1,
  size: 20,
  opTypes: [],
  startTime: null,
  endTime: null,
  keyword: '',
  sortBy: 'createdAt',
  sortOrder: 'desc'
})

const shortcuts = [
  { text: '最近一小时', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000); return [start, end] } },
  { text: '最近 24 小时', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24); return [start, end] } },
  { text: '最近一周', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } }
]

const drawer = ref({
  visible: false,
  data: null
})

const formattedJson = computed(() => {
  if (!drawer.value.data || !drawer.value.data.opDetail) return '{}'
  try {
    return JSON.stringify(JSON.parse(drawer.value.data.opDetail), null, 2)
  } catch (e) {
    return drawer.value.data.opDetail
  }
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = { ...queryForm.value }
    if (params.opTypes && params.opTypes.length > 0) {
      params.opTypes = params.opTypes.join(',')
    } else {
      delete params.opTypes
    }
    const res = await getLogs(params)
    logs.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    ElMessage.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.value.page = 1
  fetchLogs()
}

const resetQuery = () => {
  queryForm.value = {
    page: 1,
    size: 20,
    opTypes: [],
    startTime: null,
    endTime: null,
    keyword: '',
    sortBy: 'createdAt',
    sortOrder: 'desc'
  }
  timeRange.value = []
  fetchLogs()
}

const handleTimeRangeChange = (val) => {
  if (val) {
    queryForm.value.startTime = val[0]
    queryForm.value.endTime = val[1]
  } else {
    queryForm.value.startTime = null
    queryForm.value.endTime = null
  }
  handleSearch()
}

const handleSortChange = ({ prop, order }) => {
  if (!order) {
    queryForm.value.sortBy = 'createdAt'
    queryForm.value.sortOrder = 'desc'
  } else {
    queryForm.value.sortBy = prop
    queryForm.value.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  }
  fetchLogs()
}

const handleSizeChange = (val) => {
  queryForm.value.size = val
  fetchLogs()
}

const handleCurrentChange = (val) => {
  queryForm.value.page = val
  fetchLogs()
}

const getOpTypeLabel = (type) => {
  const map = {
    'OFFLINE': '活动下线',
    'WHITELIST_ADD': '白名单添加',
    'WHITELIST_REMOVE': '白名单删除',
    'DELETE': '删除记录'
  }
  return map[type] || type
}

const getOpTypeTag = (type) => {
  const map = {
    'OFFLINE': 'warning',
    'WHITELIST_ADD': 'success',
    'WHITELIST_REMOVE': 'info',
    'DELETE': 'danger'
  }
  return map[type] || ''
}

const showDetail = (row) => {
  drawer.value.data = row
  drawer.value.visible = true
}

const copyJson = () => {
  navigator.clipboard.writeText(formattedJson.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

const refreshWithNewLog = () => {
  newLogAvailable.value = false
  queryForm.value.page = 1
  queryForm.value.sortBy = 'createdAt'
  queryForm.value.sortOrder = 'desc'
  fetchLogs()
}

const initWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname
  const port = '8080' // Standard Spring Boot port
  ws = new WebSocket(`${protocol}//${host}:${port}/ws/logs`)

  ws.onmessage = (event) => {
    if (event.data === 'NEW_LOG') {
      newLogAvailable.value = true
    }
  }

  ws.onclose = () => {
    // Attempt reconnect after 5 seconds
    setTimeout(initWebSocket, 5000)
  }
}

onMounted(() => {
  if (route.query && route.query.keyword && !queryForm.value.keyword) {
    queryForm.value.keyword = String(route.query.keyword)
  }
  fetchLogs()
  initWebSocket()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
.log-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.filter-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.drawer-content {
  padding: 20px;
}
.json-detail {
  margin-top: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.json-header {
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.json-pre {
  margin: 0;
  padding: 15px;
  max-height: 500px;
  overflow: auto;
  background-color: #fff;
  font-family: monospace;
  font-size: 13px;
}
.error-text {
  color: #f56c6c;
}
</style>
