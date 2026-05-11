<template>
  <div class="documents-container">
    <div class="toolbar">
      <el-button type="primary" @click="showUpload = true">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
    </div>
    <el-table :data="documents" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="fileSize" label="大小" width="120">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="warning">处理中</el-tag>
          <el-tag v-else-if="row.status === 1" type="success">完成</el-tag>
          <el-tag v-else type="danger">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunksProcessed" label="分块数" width="100" />
      <el-table-column prop="createTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadDocuments"
      @current-change="loadDocuments"
      style="margin-top: 20px; justify-content: flex-end"
    />
    <el-dialog v-model="showUpload" title="上传文档" width="500px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".pdf,.docx,.doc,.txt"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 pdf、docx、doc、txt 格式
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploadLoading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocuments, uploadDocument, deleteDocument } from '@/api'

const loading = ref(false)
const uploadLoading = ref(false)
const showUpload = ref(false)
const uploadRef = ref(null)
const documents = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const uploadForm = reactive({
  title: '',
  file: null
})

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getDocuments({ pageNum: pageNum.value, pageSize: pageSize.value })
    documents.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  uploadForm.file = file.raw
  if (!uploadForm.title) {
    uploadForm.title = file.name.substring(0, file.name.lastIndexOf('.'))
  }
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

const handleUpload = async () => {
  if (!uploadForm.title || !uploadForm.file) {
    ElMessage.warning('请填写标题和选择文件')
    return
  }

  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('title', uploadForm.title)
    formData.append('file', uploadForm.file)
    await uploadDocument(formData)
    ElMessage.success('上传成功，文档正在处理中')
    showUpload.value = false
    uploadForm.title = ''
    uploadForm.file = null
    uploadRef.value?.clearFiles()
    loadDocuments()
  } catch (e) {
  } finally {
    uploadLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${row.title}"吗？`, '提示', {
      type: 'warning'
    })
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (e) {
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.documents-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
