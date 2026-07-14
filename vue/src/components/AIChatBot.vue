<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ChatDotSquare, Close, Promotion } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { serverHost } from '../../config/config.default'
import { marked } from 'marked'

// === State ===
const visible = ref(false)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const sessionId = ref('')
const sessions = ref([])
const chatPanelRef = ref(null)

// === Toggle panel ===
const toggleChat = () => {
  visible.value = !visible.value
}

// === Load sessions on mount ===
onMounted(async () => {
  const account = getAccount()
  if (account.id) {
    await loadSessions()
  }
})

// === Helper: get current user from localStorage ===
const getAccount = () => {
  try {
    return JSON.parse(localStorage.getItem('account') || '{}')
  } catch {
    return {}
  }
}

// === Load sessions ===
const loadSessions = async () => {
  try {
    const res = await request.get('/ai/sessions')
    if (res.code === '200') {
      sessions.value = res.data || []
    }
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

// === Load messages for a session ===
const loadMessages = async (sid) => {
  sessionId.value = sid
  try {
    const res = await request.get(`/ai/sessions/${sid}/messages`)
    if (res.code === '200') {
      messages.value = (res.data || []).map(m => ({
        role: m.role,
        content: m.content
      }))
    }
  } catch (e) {
    console.error('加载消息失败:', e)
  }
  await nextTick()
  scrollToBottom()
}

// === Send message (streaming) ===
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  loading.value = true

  // Add a placeholder for the AI response
  messages.value.push({ role: 'assistant', content: '' })
  const aiMsgIndex = messages.value.length - 1

  await nextTick()
  scrollToBottom()

  try {
    const account = getAccount()
    const token = account.token || ''
    const params = new URLSearchParams({
      question: text,
      sessionId: sessionId.value || ''
    })

    const response = await fetch(`${serverHost}/ai/chat/stream?${params}`, {
      headers: { 'token': token }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event: message')) continue
        if (line.startsWith('event: done')) continue
        if (line.startsWith('event: error')) continue
        if (line.startsWith('data: ')) {
          const data = line.substring(6)
          if (data.trim() === '[DONE]') continue
          messages.value[aiMsgIndex].content += data
          await nextTick()
          scrollToBottom()
        }
      }
    }
  } catch (e) {
    console.error('SSE error:', e)
    messages.value[aiMsgIndex].content = '抱歉，连接出错，请稍后重试。'
  } finally {
    loading.value = false
    await loadSessions()
  }
}

// === New conversation ===
const newConversation = async () => {
  try {
    const res = await request.post('/ai/sessions')
    if (res.code === '200') {
      sessionId.value = res.data
      messages.value = []
      await loadSessions()
    }
  } catch (e) {
    console.error('创建会话失败:', e)
  }
}

// === Auto-scroll ===
const scrollToBottom = () => {
  nextTick(() => {
    const container = chatPanelRef.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// === Render markdown ===
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch {
    return content
  }
}
</script>

<template>
  <div class="ai-chatbot-container">
    <!-- Floating toggle button -->
    <div class="chat-toggle" @click="toggleChat">
      <el-icon size="24" v-if="!visible"><ChatDotSquare /></el-icon>
      <el-icon size="24" v-else><Close /></el-icon>
    </div>

    <!-- Chat panel -->
    <transition name="slide-fade">
      <div v-if="visible" class="chat-panel">
        <!-- Header -->
        <div class="chat-header">
          <span class="chat-title">行山助手</span>
          <div class="header-actions">
            <el-button text size="small" @click="newConversation" title="新对话">
              <el-icon><Promotion /></el-icon>
            </el-button>
            <el-button text size="small" @click="toggleChat">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Messages -->
        <div ref="chatPanelRef" class="chat-messages">
          <!-- Empty state -->
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="empty-icon">
              <el-icon :size="48"><ChatDotSquare /></el-icon>
            </div>
            <p class="empty-title">你好！我是行山助手</p>
            <p class="empty-desc">可以问我关于徒步路线、装备建议、旅行攻略等问题</p>
          </div>

          <!-- Message bubbles -->
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message-bubble', msg.role === 'user' ? 'user-msg' : 'ai-msg']"
          >
            <div v-if="msg.role === 'assistant'" class="ai-avatar">
              <img src="../../config/logo.svg" alt="AI" />
            </div>
            <div class="bubble-content">
              <!-- AI message with markdown -->
              <div
                v-if="msg.role === 'assistant' && msg.content"
                class="markdown-body"
                v-html="renderMarkdown(msg.content)"
              ></div>
              <!-- Loading dots -->
              <div
                v-else-if="msg.role === 'assistant' && !msg.content && loading"
                class="loading-dots"
              >
                <span></span><span></span><span></span>
              </div>
              <!-- User message -->
              <div v-else>{{ msg.content }}</div>
            </div>
          </div>
        </div>

        <!-- Input area -->
        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            :disabled="loading"
            placeholder="输入你的问题..."
            @keydown.enter.prevent="sendMessage"
          />
          <el-button
            type="danger"
            :loading="loading"
            @click="sendMessage"
            class="send-btn"
          >
            发送
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
$primary-color: #d54941;

.ai-chatbot-container {
  position: fixed;
  bottom: 80px;
  right: 30px;
  z-index: 9999;
}

.chat-toggle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: $primary-color;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(213, 73, 65, 0.4);
  transition: transform 0.2s;
  &:hover { transform: scale(1.05); }
}

.chat-panel {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 380px;
  height: 560px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: $primary-color;
  color: white;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  .chat-title {
    font-weight: 600;
    font-size: 16px;
  }

  .header-actions {
    display: flex;
    gap: 4px;
  }

  :deep(.el-button) {
    color: white;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fa;
}

.chat-empty {
  text-align: center;
  margin-top: 80px;
  color: #999;

  .empty-icon {
    margin-bottom: 16px;
    color: #ccc;
  }

  .empty-title {
    font-size: 16px;
    color: #666;
    margin: 0 0 8px 0;
  }

  .empty-desc {
    font-size: 13px;
    color: #999;
    margin: 0;
    line-height: 1.6;
  }
}

.message-bubble {
  display: flex;
  margin-bottom: 16px;

  &.user-msg {
    justify-content: flex-end;
  }

  &.ai-msg {
    justify-content: flex-start;
  }
}

.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 8px;
  flex-shrink: 0;
  background: white;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.bubble-content {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;

  .user-msg & {
    background: $primary-color;
    color: white;
    border-bottom-right-radius: 4px;
  }

  .ai-msg & {
    background: white;
    color: #333;
    border: 1px solid #eee;
    border-bottom-left-radius: 4px;
  }
}

// Loading dots animation
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #999;
    animation: dot-bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: white;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;

  :deep(.el-textarea__inner) {
    border-radius: 8px;
    resize: none;
  }

  .send-btn {
    flex-shrink: 0;
    height: 36px;
  }
}

// Slide-fade transition
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

// Markdown styling
:deep(.markdown-body) {
  h1, h2, h3, h4 {
    font-size: inherit;
    margin: 8px 0 4px;
  }

  p {
    margin: 4px 0;
  }

  code {
    background: #f0f0f0;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
  }

  pre code {
    display: block;
    padding: 12px;
    overflow-x: auto;
    background: #f5f5f5;
    border-radius: 8px;
  }

  ul, ol {
    padding-left: 20px;
    margin: 4px 0;
  }

  a {
    color: $primary-color;
  }

  strong {
    font-weight: 600;
  }

  blockquote {
    border-left: 3px solid $primary-color;
    margin: 8px 0;
    padding: 4px 12px;
    color: #666;
    background: #f9f9f9;
    border-radius: 0 4px 4px 0;
  }
}
</style>