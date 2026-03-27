<template>
  <div class="my-registrations">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" clearable @change="handleSearch">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="$router.push('/checkin/scan')">扫码签到</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="rows" v-loading="loading" border style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="activityId" label="活动ID" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'">
            {{ statusLabelMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditReason" label="审核意见" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="报名时间" width="180" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button
            link
            type="danger"
            @click="handleCancel(row)"
            v-if="row.status === 'PENDING' || row.status === 'APPROVED'"
          >
            取消报名
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
        <el-descriptions-item label="用户ID">{{ detailDialog.data.userId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailDialog.data.status }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detailDialog.data.auditBy }}</el-descriptions-item>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { cancelRegistration, getMyRegistrations, getRegistrationDetail } from '../../api/registration'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const queryForm = ref({
  status: '',
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

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getMyRegistrations({
      page: queryForm.value.page,
      size: queryForm.value.size,
      status: queryForm.value.status || undefined
    })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.value.page = 1
  fetchList()
}

const resetQuery = () => {
  queryForm.value.status = ''
  queryForm.value.page = 1
  queryForm.value.size = 10
  fetchList()
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

const handleCancel = async (row) => {
  await ElMessageBox.confirm(`确认取消报名（ID: ${row.id}）？`, '提示', { type: 'warning' })
  await cancelRegistration(row.id)
  fetchList()
}

const formatJson = (raw) => {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch (e) {
    return raw
  }
}

onMounted(() => {
  fetchList()
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

