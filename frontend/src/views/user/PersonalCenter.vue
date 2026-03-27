<template>
  <div class="personal-center">
    <el-page-header content="个人中心" style="margin-bottom: 20px" @back="goBack" />

    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Activity Calendar -->
      <el-tab-pane label="活动日历" name="calendar">
        <div class="calendar-controls">
          <el-button-group>
            <el-button :type="calendarView === 'month' ? 'primary' : ''" @click="calendarView = 'month'">月视图</el-button>
            <el-button :type="calendarView === 'week' ? 'primary' : ''" @click="calendarView = 'week'">周视图</el-button>
          </el-button-group>
          <div class="nav-buttons">
            <el-button @click="navigateCalendar(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
            <span class="current-period">{{ currentPeriodLabel }}</span>
            <el-button @click="navigateCalendar(1)"><el-icon><ArrowRight /></el-icon></el-button>
            <el-button size="small" @click="goToday">今天</el-button>
          </div>
        </div>

        <div class="calendar-legend">
          <span class="legend-item"><span class="dot dot-pending"></span>报名中(待审核)</span>
          <span class="legend-item"><span class="dot dot-approved"></span>已通过</span>
          <span class="legend-item"><span class="dot dot-completed"></span>已完成</span>
          <span class="legend-item"><span class="dot dot-canceled"></span>已取消/拒绝</span>
        </div>

        <!-- Month View -->
        <div v-if="calendarView === 'month'" class="month-grid">
          <div class="weekday-header" v-for="d in weekDays" :key="d">{{ d }}</div>
          <div 
            v-for="(cell, idx) in monthCells" 
            :key="idx" 
            class="day-cell"
            :class="{ 'other-month': !cell.currentMonth, 'today': cell.isToday }"
          >
            <div class="day-number">{{ cell.day }}</div>
            <div 
              v-for="evt in cell.events" 
              :key="evt.id" 
              class="event-bar"
              :class="'event-' + evt.statusClass"
              :title="evt.title + ' (' + evt.status + ')'"
            >
              {{ evt.title }}
            </div>
          </div>
        </div>

        <!-- Week View -->
        <div v-if="calendarView === 'week'" class="week-grid">
          <div v-for="(cell, idx) in weekCells" :key="idx" class="week-day-cell" :class="{ 'today': cell.isToday }">
            <div class="week-day-header">
              <span class="week-day-name">{{ cell.dayName }}</span>
              <span class="week-day-num">{{ cell.day }}</span>
            </div>
            <div 
              v-for="evt in cell.events" 
              :key="evt.id"
              class="event-bar"
              :class="'event-' + evt.statusClass"
              :title="evt.title + ' (' + evt.status + ')'"
            >
              {{ evt.title }}
            </div>
            <div v-if="cell.events.length === 0" class="no-events">无活动</div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 2: Profile -->
      <el-tab-pane label="个人信息" name="profile">
        <el-card shadow="never" style="max-width: 600px; margin: 0 auto;">
          <div class="avatar-section">
            <el-avatar :size="80" :src="profileForm.avatarUrl || defaultAvatar">
              {{ profileForm.nickname?.charAt(0) || profileForm.username?.charAt(0) || '?' }}
            </el-avatar>
            <div class="avatar-info">
              <h3>{{ profileForm.nickname || profileForm.username }}</h3>
              <el-tag size="small">{{ roleName }}</el-tag>
            </div>
          </div>
          <el-divider />
          <el-form :model="profileForm" label-width="100px" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="设置昵称" />
            </el-form-item>
            <el-form-item label="头像链接">
              <el-input v-model="profileForm.avatarUrl" placeholder="头像图片 URL" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="邮箱地址" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="手机号码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="savingProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- Tab 3: Password -->
      <el-tab-pane label="修改密码" name="password">
        <el-card shadow="never" style="max-width: 500px; margin: 0 auto;">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitPassword" :loading="changingPassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getUserInfo, updateUserInfo, changePassword } from '../../api/user'
import { getMyRegistrations } from '../../api/registration'

const router = useRouter()
const activeTab = ref('calendar')
const calendarView = ref('month')
const currentDate = ref(new Date())
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// ---- Calendar Logic ----
const registrations = ref([])
const calendarLoading = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const getStatusClass = (status) => {
  if (status === 'PENDING') return 'pending'
  if (status === 'APPROVED') return 'approved'
  if (status === 'COMPLETED') return 'completed'
  return 'canceled' // REJECTED, CANCELED
}

const currentPeriodLabel = computed(() => {
  const d = currentDate.value
  if (calendarView.value === 'month') {
    return `${d.getFullYear()}年${d.getMonth() + 1}月`
  }
  const startOfWeek = getStartOfWeek(d)
  const endOfWeek = new Date(startOfWeek)
  endOfWeek.setDate(endOfWeek.getDate() + 6)
  return `${startOfWeek.getMonth() + 1}/${startOfWeek.getDate()} - ${endOfWeek.getMonth() + 1}/${endOfWeek.getDate()}`
})

const navigateCalendar = (dir) => {
  const d = new Date(currentDate.value)
  if (calendarView.value === 'month') {
    d.setMonth(d.getMonth() + dir)
  } else {
    d.setDate(d.getDate() + dir * 7)
  }
  currentDate.value = d
}

const goToday = () => { currentDate.value = new Date() }

const goBack = () => {
  router.back()
}

const getStartOfWeek = (date) => {
  const d = new Date(date)
  const day = d.getDay()
  d.setDate(d.getDate() - day)
  d.setHours(0, 0, 0, 0)
  return d
}

const isToday = (date) => {
  const t = new Date()
  return date.getFullYear() === t.getFullYear() && date.getMonth() === t.getMonth() && date.getDate() === t.getDate()
}

const getEventsForDate = (date) => {
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return registrations.value
    .filter(r => {
      if (!r.activityStartTime) return false
      const start = r.activityStartTime.substring(0, 10)
      const end = r.activityEndTime ? r.activityEndTime.substring(0, 10) : start
      return dateStr >= start && dateStr <= end
    })
    .map(r => ({
      id: r.id,
      title: r.activityTitle || `活动#${r.activityId}`,
      status: r.status,
      statusClass: getStatusClass(r.status)
    }))
}

const monthCells = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startPad = firstDay.getDay()
  const cells = []

  // Previous month padding
  for (let i = startPad - 1; i >= 0; i--) {
    const d = new Date(year, month, -i)
    cells.push({ day: d.getDate(), currentMonth: false, isToday: false, events: getEventsForDate(d) })
  }

  // Current month
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const d = new Date(year, month, i)
    cells.push({ day: i, currentMonth: true, isToday: isToday(d), events: getEventsForDate(d) })
  }

  // Next month padding to fill 6 rows
  const remaining = 42 - cells.length
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(year, month + 1, i)
    cells.push({ day: i, currentMonth: false, isToday: false, events: getEventsForDate(d) })
  }

  return cells
})

const weekCells = computed(() => {
  const startOfWeek = getStartOfWeek(currentDate.value)
  const cells = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(startOfWeek)
    d.setDate(d.getDate() + i)
    cells.push({
      day: d.getDate(),
      dayName: weekDays[i],
      isToday: isToday(d),
      events: getEventsForDate(d)
    })
  }
  return cells
})

const loadRegistrations = async () => {
  calendarLoading.value = true
  try {
    const res = await getMyRegistrations({ page: 1, size: 500 })
    registrations.value = res.data.records || []
  } catch (e) {
    console.error('Failed to load registrations for calendar', e)
  } finally {
    calendarLoading.value = false
  }
}

// ---- Profile Logic ----
const profileForm = reactive({
  username: '',
  nickname: '',
  avatarUrl: '',
  email: '',
  phone: ''
})
const savingProfile = ref(false)
const roleName = ref('')

const loadProfile = async () => {
  try {
    const res = await getUserInfo()
    const u = res.data
    profileForm.username = u.username
    profileForm.nickname = u.nickname || ''
    profileForm.avatarUrl = u.avatarUrl || ''
    profileForm.email = u.email || ''
    profileForm.phone = u.phone || ''
    if (u.roles && u.roles.length > 0) {
      const nameMap = {
        ROLE_SUPER_ADMIN: '超级管理员',
        ROLE_COUNSELOR: '辅导员',
        ROLE_CLUB_OWNER: '社团负责人',
        ROLE_STUDENT: '学生'
      }
      roleName.value = nameMap[u.roles[0].roleCode] || u.roles[0].roleName
    }
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  }
}

const saveProfile = async () => {
  savingProfile.value = true
  try {
    await updateUserInfo({
      nickname: profileForm.nickname,
      avatarUrl: profileForm.avatarUrl,
      email: profileForm.email,
      phone: profileForm.phone
    })
    ElMessage.success('个人信息已更新')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingProfile.value = false
  }
}

// ---- Password Logic ----
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changingPassword = ref(false)

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const submitPassword = async () => {
  await passwordFormRef.value.validate()
  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '密码修改失败，请检查原密码')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadRegistrations()
})
</script>

<style scoped>
.personal-center {
  padding: 20px;
}

/* Calendar Controls */
.calendar-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.nav-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}
.current-period {
  font-size: 16px;
  font-weight: 600;
  min-width: 150px;
  text-align: center;
}

/* Legend */
.calendar-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  font-size: 13px;
  color: #606266;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.dot-pending { background: #E6A23C; }
.dot-approved { background: #409EFF; }
.dot-completed { background: #67C23A; }
.dot-canceled { background: #909399; }

/* Month Grid */
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border: 1px solid #ebeef5;
}
.weekday-header {
  text-align: center;
  padding: 8px;
  background: #f5f7fa;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid #ebeef5;
}
.day-cell {
  min-height: 90px;
  border: 1px solid #ebeef5;
  padding: 4px;
  overflow: hidden;
}
.day-cell.other-month {
  background: #fafafa;
  color: #c0c4cc;
}
.day-cell.today {
  background: #ecf5ff;
}
.day-number {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
}

/* Week Grid */
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}
.week-day-cell {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
  min-height: 150px;
}
.week-day-cell.today {
  border-color: #409EFF;
  background: #ecf5ff;
}
.week-day-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}
.week-day-name {
  font-weight: 600;
}
.no-events {
  color: #c0c4cc;
  font-size: 12px;
  text-align: center;
  margin-top: 20px;
}

/* Event Bar */
.event-bar {
  font-size: 11px;
  padding: 2px 6px;
  margin-bottom: 3px;
  border-radius: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: default;
}
.event-pending { background: #fdf6ec; color: #E6A23C; border-left: 3px solid #E6A23C; }
.event-approved { background: #ecf5ff; color: #409EFF; border-left: 3px solid #409EFF; }
.event-completed { background: #f0f9eb; color: #67C23A; border-left: 3px solid #67C23A; }
.event-canceled { background: #f4f4f5; color: #909399; border-left: 3px solid #909399; }

/* Profile Section */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}
.avatar-info h3 {
  margin: 0 0 8px 0;
}
</style>
