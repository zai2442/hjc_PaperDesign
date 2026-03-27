<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <span>用户角色管理</span>
      </template>
      
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120"></el-table-column>
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
        <el-table-column label="操作" width="200">
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
    <el-dialog title="分配角色" v-model="assignDialogVisible">
      <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const users = ref([])
const allRoles = ref([])
const assignDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoleIds = ref([])

const fetchUsers = async () => {
  try {
    const userRole = localStorage.getItem('user_role')
    if (userRole === 'ROLE_SUPER_ADMIN') {
      const res = await request.get('/users')
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
  currentUser.value = user
  selectedRoleIds.value = user.roles ? user.roles.map(r => r.id) : []
  assignDialogVisible.value = true
}

const submitAssignRole = async () => {
  try {
    await request.post('/roles/assign', {
      userId: currentUser.value.id,
      roleIds: selectedRoleIds.value
    })
    ElMessage.success('Role Assigned Successfully')
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
</style>
