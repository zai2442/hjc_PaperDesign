<template>
  <div class="admin-registrations">
    <el-page-header content="报名管理" style="margin-bottom: 20px" @back="goBack" />
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="活动">
          <el-select
            v-model="queryForm.activityId"
            filterable
            clearable
            placeholder="选择活动"
            style="width: 260px"
            @change="handleSearch"
          >
            <el-option v-for="a in activityOptions" :key="a.id" :label="`${a.id} - ${a.title}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" clearable @change="handleSearch">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="queryForm.keyword" placeholder="活动名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" plain @click="refreshStats">刷新统计</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="12" style="margin: 12px 0" v-if="stats">
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="总报名" :value="stats.total" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="待审核" :value="stats.pending" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="已通过" :value="stats.approved" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="已拒绝" :value="stats.rejected" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="已取消" :value="stats.canceled" />
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <el-statistic title="已完成" :value="stats.completed" />
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="rows" v-loading="loading" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="activityId" label="活动ID" width="100" />
      <el-table-column prop="activityTitle" label="活动名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'">
            {{ statusLabelMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditReason" label="审核意见" min-width="200" show-overflow-tooltip />
      <el-table-column prop="auditByName" label="审核人" width="120" />
      <el-table-column prop="auditAt" label="审核时间" width="180" />
      <el-table-column prop="createdAt" label="报名时间" width="180" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button link type="success" v-if="row.status === 'PENDING'" @click="openAudit(row, 'APPROVED')">
            通过
          </el-button>
          <el-button link type="danger" v-if="row.status === 'PENDING'" @click="openAudit(row, 'REJECTED')">
            驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryForm.page"
      v-model:page-size="queryForm.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      style="margin-top: 20px; justify-content: flex-end"
    />

    <el-dialog v-model="detailDialog.visible" title="报名详情" width="700px">
      <el-descriptions v-if="detailDialog.data" :column="2" border>
        <el-descriptions-item label="ID">{{ detailDialog.data.id }}</el-descriptions-item>
        <el-descriptions-item label="活动ID">{{ detailDialog.data.activityId }}</el-descriptions-item>
        <el-descriptions-item label="活动名称">{{ detailDialog.data.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailDialog.data.username }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailDialog.data.status }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detailDialog.data.auditByName || detailDialog.data.auditBy }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailDialog.data.auditAt }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ detailDialog.data.auditReason }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ detailDialog.data.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailDialog.data.updatedAt }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailDialog.data && detailDialog.data.extraData" style="margin-top: 12px">
        <el-collapse>
          <el-collapse-item title="报名附加信息">
            <pre class="json-pre">{{ formatJson(detailDialog.data.extraData) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditDialog.visible" :title="auditDialog.title" width="520px">
      <el-form :model="auditDialog.form" label-width="90px">
        <el-form-item label="审核意见">
          <el-input v-model="auditDialog.form.reason" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityList } from '../../api/activity'
import { auditRegistration, getAdminRegistrations, getRegistrationDetail, getRegistrationStats } from '../../api/registration'

const route = useRoute()
const router = useRouter()

const goBack = () => {
  router.back()
}

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const stats = ref(null)

const activityOptions = ref([])

const queryForm = ref({
  activityId: undefined,
  status: '',
  keyword: '',
  page: 1,
  size: 10
})

const statusLabelMap = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELED: '已取消',
  COMPLETED: '已完成'
}

const statusTypeMap = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELED: 'info',
  COMPLETED: 'success'
}

const detailDialog = ref({
  visible: false,
  data: null
})

const auditDialog = ref({
  visible: false,
  title: '',
  id: null,
  status: '',
  form: {
    reason: ''
  }
})

const fetchActivities = async () => {
  const res = await getActivityList({ page: 1, size: 200 })
  activityOptions.value = res.data.records || []
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminRegistrations({
      activityId: queryForm.value.activityId,
      status: queryForm.value.status || undefined,
      keyword: queryForm.value.keyword || undefined,
      page: queryForm.value.page,
      size: queryForm.value.size
    })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const refreshStats = async () => {
  const res = await getRegistrationStats({ activityId: queryForm.value.activityId })
  stats.value = res.data
}

const handleSearch = async () => {
  queryForm.value.page = 1
  await fetchList()
  await refreshStats()
  if (queryForm.value.activityId) {
    router.replace({ path: route.path, query: { activityId: String(queryForm.value.activityId) } })
  } else {
    router.replace({ path: route.path, query: {} })
  }
}

const resetQuery = () => {
  queryForm.value.status = ''
  queryForm.value.keyword = ''
  queryForm.value.page = 1
  queryForm.value.size = 10
  fetchList()
  refreshStats()
}

const handleSizeChange = (size) => {
  queryForm.value.size = size
  queryForm.value.page = 1
  fetchList()
}

const handleCurrentChange = (page) => {
  queryForm.value.page = page
  fetchList()
}

const openDetail = async (id) => {
  const res = await getRegistrationDetail(id)
  detailDialog.value.data = res.data
  detailDialog.value.visible = true
}

const openAudit = (row, status) => {
  auditDialog.value.id = row.id
  auditDialog.value.status = status
  auditDialog.value.form.reason = ''
  auditDialog.value.title = status === 'APPROVED' ? '审核通过' : '审核驳回'
  auditDialog.value.visible = true
}

const confirmAudit = async () => {
  await auditRegistration(auditDialog.value.id, {
    status: auditDialog.value.status,
    reason: auditDialog.value.form.reason
  })
  auditDialog.value.visible = false
  ElMessage.success('操作成功')
  fetchList()
  refreshStats()
}

const formatJson = (raw) => {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch (e) {
    return raw
  }
}

onMounted(async () => {
  await fetchActivities()
  const activityId = route.query.activityId
  if (activityId) {
    queryForm.value.activityId = Number(activityId)
  }
  await handleSearch()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 10px;
}
.json-pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
</style>

