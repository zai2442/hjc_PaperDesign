<template>
  <div class="user-management">
    <el-page-header content="用户角色管理" style="margin-bottom: 20px" @back="goBack" />
    <el-card>
      <template #header>
        <div class="header-content">
          <span>用户列表</span>
          <div class="controls">
            <el-button 
              type="success" 
              :disabled="selectedUsers.length === 0" 
              @click="handleBatchAssignRole"
              style="margin-right: 15px;">
              批量分配角色
            </el-button>
            <el-input
              v-model="searchQuery.username"
              placeholder="搜索用户名"
              clearable
              style="width: 200px; margin-right: 15px;"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button icon="Search" @click="handleSearch" />
              </template>
            </el-input>
            <el-checkbox v-model="searchQuery.sortByRole" @change="handleSearch">
              按角色排序
            </el-checkbox>
          </div>
        </div>
      </template>
      
      <el-table :data="users" style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" :selectable="selectable"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="{ row }">
            <el-link type="primary" @click="showUserDetails(row)">{{ row.username }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
        <el-table-column prop="email" label="邮箱"></el-table-column>
        <el-table-column prop="roles" label="角色">
          <template #default="{ row }">
            <el-tag 
              v-for="role in (row.roles || [])" 
              :key="role.id" 
              size="small"
              style="margin-right: 5px;">
              {{ role.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              @click="handleAssignRole(row)"
              :disabled="row.username === 'admin'">
              分配角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分配角色对话框 -->
    <el-dialog :title="isBatch ? '批量分配角色' : '分配角色'" v-model="assignDialogVisible" width="400px">
      <div v-if="isBatch" style="margin-bottom: 15px; color: #666;">
        已选择 {{ selectedUsers.length }} 个用户
      </div>
      <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
        <el-option
          v-for="role in allRoles"
          :key="role.id"
          :label="role.roleName"
          :value="role.id">
        </el-option>
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog title="用户详情" v-model="userDetailVisible" width="500px">
      <el-descriptions v-if="userDetailData" :column="1" border>
        <el-descriptions-item label="ID">{{ userDetailData.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userDetailData.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userDetailData.nickname || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userDetailData.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ userDetailData.phone || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag 
            v-for="role in (userDetailData.roles || [])" 
            :key="role.id" 
            size="small"
            style="margin-right: 5px;">
            {{ role.roleName }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="userDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const users = ref([])

const goBack = () => {
  router.back()
}
const allRoles = ref([])
const assignDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoleIds = ref([])
const searchQuery = reactive({
  username: '',
  sortByRole: false
})

const selectedUsers = ref([])
const isBatch = ref(false)
const userDetailVisible = ref(false)
const userDetailData = ref(null)

const handleSelectionChange = (val) => {
  selectedUsers.value = val
}

const selectable = (row) => {
  return row.username !== 'admin'
}

const handleSearch = () => {
  fetchUsers()
}

const fetchUsers = async () => {
  try {
    const userRole = localStorage.getItem('user_role')
    if (userRole === 'ROLE_SUPER_ADMIN') {
      const res = await request.get('/users', { params: searchQuery })
      users.value = res.data
    } else {
      const res = await request.get('/users/me')
      users.value = [res.data] 
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchRoles = async () => {
  try {
    const res = await request.get('/roles')
    allRoles.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const handleAssignRole = (user) => {
  isBatch.value = false
  currentUser.value = user
  selectedRoleIds.value = user.roles ? user.roles.map(r => r.id) : []
  assignDialogVisible.value = true
}

const handleBatchAssignRole = () => {
  isBatch.value = true
  selectedRoleIds.value = []
  assignDialogVisible.value = true
}

const showUserDetails = (user) => {
  userDetailData.value = user
  userDetailVisible.value = true
}

const submitAssignRole = async () => {
  try {
    const data = {
      roleIds: selectedRoleIds.value
    }
    if (isBatch.value) {
      data.userIds = selectedUsers.value.map(u => u.id)
    } else {
      data.userId = currentUser.value.id
    }

    await request.post('/roles/assign', data)
    ElMessage.success(isBatch.value ? 'Batch Roles Assigned Successfully' : 'Role Assigned Successfully')
    assignDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.controls {
  display: flex;
  align-items: center;
}
</style>
