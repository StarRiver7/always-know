<template>
  <div class="chat-container">
    <div class="chat-header">
      <el-select v-model="selectedDocuments" multiple placeholder="选择文档（默认全部）" style="width: 300px">
        <el-option v-for="doc in documents" :key="doc.id" :label="doc.title" :value="doc.id" />
      </el-select>
    </div>
    <div class="chat-messages" ref="messagesRef">
      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
        <div class="avatar">
          <el-icon v-if="msg.role === 'user'"><User /></el-icon>
          <el-icon v-else><Promotion /></el-icon>
        </div>
        <div class="content">
          <div class="bubble">{{ msg.content }}</div>
          <div v-if="msg.sources && msg.sources.length > 0" class="sources">
            <div class="source-title">来源:</div>
            <div v-for="(src, i) in msg.sources" :key="i" class="source-item">
              {{ src.content }}
            </div>
          </div>
        </div>
      </div>
      <div v-if="loading" class="message assistant">
        <div class="avatar"><el-icon><Promotion /></el-icon></div>
        <div class="content">
          <div class="bubble">思考中...</div>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <el-input v-model="query" type="textarea" :rows="3" placeholder="请输入问题..." @keydown.ctrl.enter="handleSend" />
      <el-button type="primary" @click="handleSend" :loading="loading" style="margin-top: 10px">发送 (Ctrl+Enter)</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { chat, getDocuments } from '@/api'

const messagesRef = ref(null)
const query = ref('')
const loading = ref(false)
const messages = ref([])
const documents = ref([])
const selectedDocuments = ref([])

const loadDocuments = async () => {
  const res = await getDocuments({ pageNum: 1, pageSize: 100 })
  documents.value = res.data.records || []
}

const handleSend = async () => {
  if (!query.value.trim() || loading.value) return

  const userMsg = { role: 'user', content: query.value }
  messages.value.push(userMsg)
  const q = query.value
  query.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await chat({
      query: q,
      documentIds: selectedDocuments.value.length > 0 ? selectedDocuments.value : null
    })
    messages.value.push({
      role: 'assistant',
      content: res.data.answer,
      sources: res.data.sources
    })
  } catch (e) {
    console.error('Chat error:', e)
    messages.value.push({
      role: 'assistant',
      content: `抱歉，回答失败：${e.response?.data?.message || e.message || '未知错误'}`
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.message.user .avatar {
  background: #67c23a;
}

.content {
  max-width: 70%;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}

.message.user .bubble {
  background: #409eff;
  color: white;
}

.message.assistant .bubble {
  background: #f5f7fa;
  color: #333;
}

.sources {
  margin-top: 10px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  font-size: 12px;
}

.source-title {
  font-weight: bold;
  margin-bottom: 5px;
  color: #909399;
}

.source-item {
  padding: 5px 0;
  border-bottom: 1px solid #e5e7eb;
  color: #606266;
}

.source-item:last-child {
  border-bottom: none;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}
</style>
