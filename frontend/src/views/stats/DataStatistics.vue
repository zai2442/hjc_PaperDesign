<template>
  <div class="stats-container">
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>活动总数</span>
              <el-icon><Calendar /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <span class="number">{{ overview.totalActivities }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>累计报名</span>
              <el-icon><User /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <span class="number">{{ overview.totalRegistrations }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>累计签到</span>
              <el-icon><Checked /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <span class="number">{{ overview.totalCheckIns }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <span>整体签到率</span>
              <el-icon><PieChart /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <span class="number">{{ (overview.overallCheckInRate * 100).toFixed(1) }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>活动趋势</span>
              <el-date-picker
                v-model="trendRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                @change="fetchTrends"
                :shortcuts="dateShortcuts"
              />
            </div>
          </template>
          <div ref="trendChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>签到率分布</span>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>活动参与排行</span>
            </div>
          </template>
          <el-table :data="ranking" style="width: 100%" stripe>
            <el-table-column type="index" label="排名" width="80" align="center" />
            <el-table-column prop="activityTitle" label="活动标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="registrationCount" label="报名人数" width="120" align="center" sortable />
            <el-table-column prop="checkInCount" label="签到人数" width="120" align="center" sortable />
            <el-table-column prop="checkInRate" label="签到率" width="150" align="center">
              <template #default="{ row }">
                <el-progress 
                  :percentage="Math.round(row.checkInRate * 100)" 
                  :status="row.checkInRate > 0.8 ? 'success' : row.checkInRate > 0.5 ? 'warning' : 'exception'"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="drillToDetail(row.activityId)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Calendar, User, Checked, PieChart } from '@element-plus/icons-vue'
import { getStatsOverview, getActivityTrends, getParticipationRanking } from '../../api/stats'
import { ElMessage } from 'element-plus'

const router = useRouter()
const overview = ref({
  totalActivities: 0,
  totalRegistrations: 0,
  totalCheckIns: 0,
  overallCheckInRate: 0
})

const trendRange = ref([
  new Date(new Date().setDate(new Date().getDate() - 30)),
  new Date()
])

const dateShortcuts = [
  { text: '最近一周', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
  { text: '最近一月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 30); return [start, end] } },
  { text: '最近三月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 90); return [start, end] } }
]

const ranking = ref([])
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

const fetchOverview = async () => {
  try {
    const res = await getStatsOverview()
    overview.value = res.data
  } catch (err) {
    ElMessage.error('加载概览数据失败')
  }
}

const fetchTrends = async () => {
  try {
    const [start, end] = trendRange.value
    const res = await getActivityTrends({
      start: start.toISOString(),
      end: end.toISOString()
    })
    updateTrendChart(res.data)
  } catch (err) {
    ElMessage.error('加载趋势数据失败')
  }
}

const fetchRanking = async () => {
  try {
    const res = await getParticipationRanking({ limit: 10 })
    ranking.value = res.data
    updatePieChart(res.data)
  } catch (err) {
    ElMessage.error('加载排行数据失败')
  }
}

const updateTrendChart = (data) => {
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map(i => i.date),
      boundaryGap: false
    },
    yAxis: { type: 'value' },
    series: [{
      data: data.map(i => i.count),
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#ecf5ff' }
        ])
      },
      lineStyle: { color: '#409EFF' }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  }
  trendChart.setOption(option)
}

const updatePieChart = (data) => {
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  
  // Calculate segments for pie chart
  const high = data.filter(i => i.checkInRate >= 0.8).length
  const mid = data.filter(i => i.checkInRate >= 0.5 && i.checkInRate < 0.8).length
  const low = data.filter(i => i.checkInRate < 0.5).length

  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      name: '签到率分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: '20', fontWeight: 'bold' } },
      data: [
        { value: high, name: '优秀 (>=80%)', itemStyle: { color: '#67C23A' } },
        { value: mid, name: '良好 (50-80%)', itemStyle: { color: '#E6A23C' } },
        { value: low, name: '待改进 (<50%)', itemStyle: { color: '#F56C6C' } }
      ]
    }]
  }
  pieChart.setOption(option)
}

const drillToDetail = (activityId) => {
  router.push(`/activity/detail/${activityId}`)
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  fetchOverview()
  fetchTrends()
  fetchRanking()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.stats-container {
  padding: 20px;
}
.overview-cards {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.card-content {
  padding: 10px 0;
}
.number {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
}
.chart-row {
  margin-bottom: 20px;
}
.chart-card {
  min-height: 450px;
}
</style>
