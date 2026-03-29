<template>
  <div class="activity-list-container">
    <el-page-header content="活动管理列表" style="margin-bottom: 20px" @back="goBack" />
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="活动名称">
          <el-input v-model="queryForm.keyword" placeholder="输入名称搜索" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" clearable @change="handleSearch">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="被驳回" value="REJECTED" />
            <el-option label="已发布" value="ONLINE" />
            <el-option label="已结束" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="handleCreate" v-if="canCreate">发布活动</el-button>
        </el-form-item>
        <el-form-item v-if="selectedIds.length > 0">
          <el-button type="warning" plain @click="handleBatchOffline">批量下线</el-button>
          <el-button type="danger" plain @click="handleBatchDelete" v-if="canAdmin">批量删除</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="activities" v-loading="loading" border style="width: 100%; margin-top: 20px" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" sortable />
      <el-table-column prop="title" label="活动名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contentType" label="分类" width="120" />
      <el-table-column label="标签" width="150">
        <template #default="{ row }">
          <div style="display: flex; flex-wrap: wrap; gap: 4px">
            <el-tag v-for="tag in row.tags" :key="tag.id" :color="tag.color" size="small" :style="{ color: 'white', border: 'none' }">
              {{ tag.name }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="150" show-overflow-tooltip />
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status]">{{ statusLabelMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="stockTotal" label="名额" width="80" />
      <el-table-column prop="stockAvailable" label="剩余" width="80" />
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button-group>
            <el-button link type="primary" @click="handleEdit(row)" v-if="canEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleSubmitReview(row)" v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">提审</el-button>
            <el-button link type="warning" @click="handleWithdraw(row)" v-if="row.status === 'PENDING_REVIEW'">撤回</el-button>
            <el-button link type="primary" @click="handleApprove(row)" v-if="canAudit && row.status === 'PENDING_REVIEW'">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)" v-if="canAudit && row.status === 'PENDING_REVIEW'">驳回</el-button>
            <el-button link type="danger" @click="handleOffline(row)" v-if="row.status === 'ONLINE'">下线</el-button>
            <el-button link type="primary" @click="handleRegistrations(row)" v-if="canManageRegistrations">报名</el-button>
            <el-button link type="success" @click="handleCheckInAdmin(row)" v-if="canManageRegistrations && row.status === 'ONLINE'">签到</el-button>
            <el-button link type="warning" @click="handleWhitelist(row)" v-if="canManageRegistrations && row.whitelistEnabled === 1">白名单</el-button>
            <el-button link type="info" @click="goSystemLogs(row)">日志</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="canAdmin">删除</el-button>
          </el-button-group>
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

    <!-- 审批/驳回对话框 -->
    <el-dialog v-model="auditDialog.visible" :title="auditDialog.title" width="500px">
      <el-form :model="auditDialog.form" label-width="80px">
        <el-form-item label="意见/理由">
          <el-input v-model="auditDialog.form.reason" type="textarea" :rows="3" placeholder="请输入意见" />
        </el-form-item>
        <el-form-item label="定时发布" v-if="auditDialog.isApprove">
          <el-date-picker v-model="auditDialog.form.publishAt" type="datetime" placeholder="选择时间，不选则立即发布" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="自动下线" v-if="auditDialog.isApprove">
          <el-date-picker v-model="auditDialog.form.offlineAt" type="datetime" placeholder="选择时间，不选则手动下线" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAudit">确定</el-button>
      </template>
    </el-dialog>


    <!-- 白名单管理对话框 -->
    <el-dialog v-model="whitelistDialog.visible" title="白名单管理" width="700px">
      <el-transfer
        v-model="whitelistDialog.selectedIds"
        :data="whitelistDialog.allUsers"
        :titles="['未加入', '已加入白名单']"
        :props="{ key: 'id', label: 'label' }"
        filterable
        filter-placeholder="搜索用户"
        v-loading="whitelistDialog.loading"
      />
      <template #footer>
        <el-button @click="whitelistDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveWhitelist" :loading="whitelistDialog.saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActivityList,
  deleteActivity,
  submitReview,
  withdrawReview,
  approveActivity,
  rejectActivity,
  offlineActivity,
  batchOfflineActivities,
  batchDeleteActivities,
  getWhitelistUserIds,
  addWhitelistUsers,
  removeWhitelistUsers
} from '../../api/activity'
import request from '../../utils/request'

const router = useRouter()

const goBack = () => {
  router.back()
}
const loading = ref(false)
const activities = ref([])
const total = ref(0)
const selectedIds = ref([])
const queryForm = ref({
  keyword: '',
  status: '',
  page: 1,
  size: 10
})

const statusLabelMap = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  REJECTED: '被驳回',
  APPROVED: '已过审',
  ONLINE: '已发布',
  OFFLINE: '已下线'
}

const statusTypeMap = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  REJECTED: 'danger',
  APPROVED: 'success',
  ONLINE: 'success',
  OFFLINE: 'info'
}


// 模拟权限判断
const userRole = localStorage.getItem('user_role') || 'STUDENT'
const canAdmin = computed(() => ['ROLE_SUPER_ADMIN'].includes(userRole))
const canAudit = computed(() => ['ROLE_SUPER_ADMIN', 'ROLE_COUNSELOR'].includes(userRole))
const canCreate = computed(() => ['ROLE_SUPER_ADMIN', 'ROLE_COUNSELOR', 'ROLE_CLUB_OWNER'].includes(userRole))
const canManageRegistrations = computed(() => ['ROLE_SUPER_ADMIN', 'ROLE_COUNSELOR', 'ROLE_CLUB_OWNER'].includes(userRole))

const canEdit = (row) => {
  if (row.status === 'PENDING_REVIEW') return false
  if (row.status === 'ONLINE') return false
  return true
}

const handleRegistrations = (row) => {
  router.push({ path: '/registration/admin', query: { activityId: row.id } })
}

const handleCheckInAdmin = (row) => {
  router.push({ path: '/checkin/admin', query: { activityId: row.id } })
}

const goSystemLogs = (row) => {
  router.push({ path: '/admin/logs', query: { keyword: String(row.id) } })
}

const fetchActivities = async () => {
  loading.value = true
  try {
    const res = await getActivityList(queryForm.value)
    activities.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.value.page = 1
  fetchActivities()
}

const resetQuery = () => {
  queryForm.value = {
    keyword: '',
    status: '',
    page: 1,
    size: 10
  }
  fetchActivities()
}

const handleCreate = () => {
  router.push('/activity/create')
}

const handleEdit = (row) => {
  router.push(`/activity/edit/${row.id}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该活动吗？', '警告', {
    type: 'warning'
  }).then(async () => {
    await deleteActivity(row.id, row.version)
    ElMessage.success('删除成功')
    fetchActivities()
  })
}

const handleSubmitReview = async (row) => {
  await submitReview(row.id, { version: row.version })
  ElMessage.success('提交成功')
  fetchActivities()
}

const handleWithdraw = async (row) => {
  await withdrawReview(row.id, { version: row.version })
  ElMessage.success('撤回成功')
  fetchActivities()
}

const handleOffline = async (row) => {
  await offlineActivity(row.id, { version: row.version })
  ElMessage.success('活动已下线')
  fetchActivities()
}

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.id)
}

const handleBatchOffline = () => {
  ElMessageBox.confirm(`确定要批量下线选中的 ${selectedIds.value.length} 个活动吗？`, '提示').then(async () => {
    await batchOfflineActivities(selectedIds.value)
    ElMessage.success('批量操作完成')
    fetchActivities()
  })
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要批量删除选中的 ${selectedIds.value.length} 个活动吗？该操作不可逆！`, '警告', { type: 'danger' }).then(async () => {
    await batchDeleteActivities(selectedIds.value)
    ElMessage.success('批量删除完成')
    fetchActivities()
  })
}

// 审批相关
const auditDialog = ref({
  visible: false,
  title: '',
  isApprove: true,
  row: null,
  form: {
    reason: '',
    publishAt: '',
    offlineAt: ''
  }
})

const handleApprove = (row) => {
  auditDialog.value = {
    visible: true,
    title: '审批通过',
    isApprove: true,
    row,
    form: { reason: '通过', publishAt: '', offlineAt: '' }
  }
}

const handleReject = (row) => {
  auditDialog.value = {
    visible: true,
    title: '审批驳回',
    isApprove: false,
    row,
    form: { reason: '', publishAt: '', offlineAt: '' }
  }
}

const confirmAudit = async () => {
  const { row, isApprove, form } = auditDialog.value
  const data = {
    version: row.version,
    reason: form.reason,
    publishAt: form.publishAt || null,
    offlineAt: form.offlineAt || null
  }
  if (isApprove) {
    await approveActivity(row.id, data)
  } else {
    await rejectActivity(row.id, data)
  }
  ElMessage.success('操作成功')
  auditDialog.value.visible = false
  fetchActivities()
}



// 白名单管理
const whitelistDialog = ref({
  visible: false,
  loading: false,
  saving: false,
  activityId: null,
  allUsers: [],
  selectedIds: [],
  originalIds: []
})

const handleWhitelist = async (row) => {
  whitelistDialog.value = {
    visible: true,
    loading: true,
    saving: false,
    activityId: row.id,
    allUsers: [],
    selectedIds: [],
    originalIds: []
  }
  try {
    const [usersRes, wlRes] = await Promise.all([
      request.get('/users'),
      getWhitelistUserIds(row.id)
    ])
    whitelistDialog.value.allUsers = usersRes.data.map(u => ({
      id: u.id,
      label: `${u.username} (ID:${u.id})`
    }))
    whitelistDialog.value.selectedIds = wlRes.data || []
    whitelistDialog.value.originalIds = [...(wlRes.data || [])]
  } catch (err) {
    ElMessage.error('获取白名单数据失败')
  } finally {
    whitelistDialog.value.loading = false
  }
}

const saveWhitelist = async () => {
  whitelistDialog.value.saving = true
  try {
    const original = new Set(whitelistDialog.value.originalIds)
    const current = new Set(whitelistDialog.value.selectedIds)
    const toAdd = whitelistDialog.value.selectedIds.filter(id => !original.has(id))
    const toRemove = whitelistDialog.value.originalIds.filter(id => !current.has(id))
    if (toAdd.length > 0) {
      await addWhitelistUsers(whitelistDialog.value.activityId, toAdd)
    }
    if (toRemove.length > 0) {
      await removeWhitelistUsers(whitelistDialog.value.activityId, toRemove)
    }
    ElMessage.success('白名单已更新')
    whitelistDialog.value.visible = false
  } catch (err) {
    ElMessage.error('保存白名单失败')
  } finally {
    whitelistDialog.value.saving = false
  }
}

const handleSizeChange = (val) => {
  queryForm.value.size = val
  fetchActivities()
}

const handleCurrentChange = (val) => {
  queryForm.value.page = val
  fetchActivities()
}

onMounted(() => {
  fetchActivities()
})
</script>

<style scoped>
.activity-list-container {
  padding: 20px;
}
.filter-card {
  margin-bottom: 20px;
}
</style>
