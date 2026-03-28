<template>
  <div class="activity-edit-container">
    <el-page-header @back="goBack" :content="isEdit ? '编辑活动' : '发布活动'" />
    
    <el-card class="form-card" v-loading="loading">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="活动名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入活动名称" maxlength="100" show-word-limit />
            </el-form-item>
            
            <el-form-item label="活动简介" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="简要描述活动内容" maxlength="500" show-word-limit />
            </el-form-item>
            
            <el-form-item label="活动封面" prop="coverUrl">
              <el-upload
                class="cover-uploader"
                action="/api/v1/admin/files/upload"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
              >
                <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-image" />
                <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
                <template #tip>
                  <div class="el-upload__tip">建议比例 16:9，大小不超过 2MB</div>
                </template>
              </el-upload>
            </el-form-item>
            
            <el-form-item label="活动详情" prop="content">
              <div class="editor-wrapper">
                <Toolbar
                  style="border-bottom: 1px solid #ccc"
                  :editor="editorRef"
                  :defaultConfig="toolbarConfig"
                  mode="default"
                />
                <Editor
                  style="height: 500px; overflow-y: hidden;"
                  v-model="form.content"
                  :defaultConfig="editorConfig"
                  mode="default"
                  @onCreated="handleCreated"
                  @onChange="handleAutoSave"
                />
              </div>
            </el-form-item>
          </el-col>
          
          <el-col :span="8">
            <el-form-item label="活动标签" prop="tagIds">
              <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width: 100%">
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id">
                  <span :style="{ color: tag.color }">{{ tag.name }}</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="活动分类" prop="contentType">
              <el-select v-model="form.contentType" placeholder="选择分类" style="width: 100%">
                <el-option label="学术讲座" value="学术讲座" />
                <el-option label="社团活动" value="社团活动" />
                <el-option label="体育赛事" value="体育赛事" />
                <el-option label="文艺演出" value="文艺演出" />
                <el-option label="志愿服务" value="志愿服务" />
              </el-select>
            </el-form-item>

            <el-form-item label="活动地点" prop="location">
              <el-input v-model="form.location" placeholder="活动举办地点" />
            </el-form-item>
            
            <el-form-item label="参与名额" prop="stockTotal">
              <el-input-number v-model="form.stockTotal" :min="0" :precision="0" />
              <span class="unit">人 (0 表示不限)</span>
            </el-form-item>

            <el-form-item label="每人限报" prop="perUserLimit">
              <el-input-number v-model="form.perUserLimit" :min="0" :precision="0" />
              <span class="unit">次 (0 表示不限)</span>
            </el-form-item>

            <el-divider content-position="left">时间设置</el-divider>

            <el-form-item label="活动开始" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            
            <el-form-item label="活动结束" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            
            <el-form-item label="报名开始" prop="regStartTime">
              <el-date-picker v-model="form.regStartTime" type="datetime" placeholder="不填则立即开始" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            
            <el-form-item label="报名结束" prop="regEndTime">
              <el-date-picker v-model="form.regEndTime" type="datetime" placeholder="不填则持续到活动结束" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>

            <el-form-item label="定时发布" prop="publishAt">
              <el-date-picker v-model="form.publishAt" type="datetime" placeholder="不填则审核通过后发布" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            
            <el-form-item label="定时下线" prop="offlineAt">
              <el-date-picker v-model="form.offlineAt" type="datetime" placeholder="不填则不自动下线" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>

            <el-form-item label="名单限制">
              <el-switch v-model="form.whitelistEnabled" :active-value="1" :inactive-value="0" active-text="开启名单过滤" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="goBack">取消</el-button>
          <el-button type="info" @click="handlePreview">预览</el-button>
          <el-button type="primary" @click="handleSave(true)" :loading="submitting">仅保存草稿</el-button>
          <el-button type="success" @click="handleSave(false)" :loading="submitting">保存并提审</el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewDialog.visible" title="活动预览" width="375px" class="preview-dialog">
      <div class="mobile-preview">
        <img v-if="form.coverUrl" :src="form.coverUrl" class="preview-cover" />
        <div class="preview-content">
          <h2 class="preview-title">{{ form.title || '活动标题' }}</h2>
          <div class="preview-meta">
            <p><el-icon><Calendar /></el-icon> {{ form.startTime || '未设置' }}</p>
            <p><el-icon><Location /></el-icon> {{ form.location || '未设置' }}</p>
          </div>
          <div class="preview-summary">{{ form.summary || '这里是活动简介...' }}</div>
          <el-divider />
          <div class="preview-detail" v-html="form.content || '这里是活动详情内容...'"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, shallowRef, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar, Location } from '@element-plus/icons-vue'
import { createActivity, updateActivity, getActivityDetail, submitReview, getTagList } from '../../api/activity'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)
const tags = ref([])

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef()

const toolbarConfig = {}
const editorConfig = { 
  placeholder: '请输入活动详情内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/v1/admin/files/upload',
      fieldName: 'file',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`
      },
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data, '', res.data)
        } else {
          ElMessage.error(res.message || '上传失败')
        }
      }
    },
    uploadVideo: {
      server: '/api/v1/admin/files/upload',
      fieldName: 'file',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`
      },
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data, '')
        } else {
          ElMessage.error(res.message || '上传失败')
        }
      }
    }
  }
}

const handleCreated = (editor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
    const editor = editorRef.value
    if (editor == null) return
    editor.destroy()
})

const form = ref({
  title: '',
  summary: '',
  coverUrl: '',
  content: '',
  contentType: '学术讲座',
  location: '',
  startTime: '',
  endTime: '',
  regStartTime: '',
  regEndTime: '',
  publishAt: '',
  offlineAt: '',
  whitelistEnabled: 0,
  stockTotal: 0,
  perUserLimit: 1,
  tagIds: [],
  version: 0
})

const rules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

const goBack = () => {
  router.push('/activity/list')
}

const fetchDetail = async () => {
  if (!isEdit.value) {
    // 检查是否有本地草稿
    const draft = localStorage.getItem('activity_draft')
    if (draft) {
      ElMessageBox.confirm('检测到上次未保存的草稿，是否恢复？', '提示').then(() => {
        form.value = { ...form.value, ...JSON.parse(draft) }
      })
    }
    return
  }
  loading.value = true
  try {
    const res = await getActivityDetail(route.params.id)
    const { activity, tags: activityTags } = res.data
    form.value = { ...activity, tagIds: activityTags.map(t => t.id) }
  } catch (err) {
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const res = await getTagList()
    tags.value = res.data
  } catch (err) {}
}

let autoSaveTimer = null
const handleAutoSave = () => {
  if (isEdit.value) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => {
    localStorage.setItem('activity_draft', JSON.stringify(form.value))
    ElMessage({ message: '草稿已自动保存', type: 'info', duration: 1000 })
  }, 2000)
}

const beforeUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJpgOrPng) ElMessage.error('只能上传 JPG/PNG/WebP 格式图片')
  if (!isLt2M) ElMessage.error('图片大小不能超过 2MB')
  return isJpgOrPng && isLt2M
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    form.value.coverUrl = res.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const handleSave = async (onlySave) => {
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    let activityId = route.params.id
    if (isEdit.value) {
      await updateActivity(activityId, form.value)
    } else {
      const res = await createActivity(form.value)
      activityId = res.data
    }

    if (!onlySave) {
      // 需要先获取最新 version
      const detailRes = await getActivityDetail(activityId)
      await submitReview(activityId, { version: detailRes.data.activity.version })
      ElMessage.success('保存并提交审核成功')
    } else {
      ElMessage.success('草稿保存成功')
    }
    localStorage.removeItem('activity_draft')
    router.push('/activity/list')
  } catch (err) {
    // 错误已由拦截器处理
  } finally {
    submitting.value = false
  }
}

const previewDialog = ref({ visible: false })
const handlePreview = () => {
  previewDialog.value.visible = true
}

onMounted(() => {
  fetchTags()
  fetchDetail()
})
</script>

<style scoped>
.activity-edit-container {
  padding: 20px;
}
.form-card {
  margin-top: 20px;
}
.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 240px;
  height: 135px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.cover-uploader:hover {
  border-color: var(--el-color-primary);
}
.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.cover-image {
  width: 240px;
  height: 135px;
  object-fit: cover;
}
.unit {
  margin-left: 10px;
  color: #999;
}
.editor-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #999;
}
.form-actions {
  margin-top: 40px;
  text-align: center;
  border-top: 1px solid #eee;
  padding-top: 20px;
}
.mobile-preview {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  font-family: sans-serif;
}
.preview-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}
.preview-content {
  padding: 15px;
}
.preview-title {
  font-size: 18px;
  margin: 0 0 10px;
}
.preview-meta {
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
}
.preview-meta p {
  margin: 4px 0;
  display: flex;
  align-items: center;
  gap: 5px;
}
.preview-summary {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}
.preview-detail {
  font-size: 14px;
  line-height: 1.6;
}
.editor-wrapper {
  border: 1px solid #ccc;
  z-index: 100;
}
</style>
