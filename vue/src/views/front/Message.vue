<script setup>
import {onMounted, ref} from 'vue'
import request from '../../utils/request'

const users = ref([])
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data
  })
}

const blogs = ref([])
const loadBlog = () => {
  request.get('/blog').then(res => {
    blogs.value = res.data
  })
}

const messages = ref([])
const loadMessage = () => {
  request.get('/message').then(res => {
    messages.value = res.data
  })
}

onMounted(() => {
  loadUser()
  loadBlog()
  loadMessage()
})
</script>

<template>
  <div class="content-container">
    <!-- 标题 -->
    <div class="header-title">
      <h2>收藏通知</h2>
    </div>

    <!-- 通知列表 -->
    <div class="notification-list">
      <div v-for="message in messages" :key="message.id" class="notification-item">
        <!-- 用户头像 -->
        <div class="user-avatar">
          <img :src="users.find(user => user.id === message.fromUserId)?.avatarUrl" :alt="users.find(user => user.id === message.fromUserId)?.nickname">
        </div>

        <!-- 通知内容 -->
        <div class="notification-content">
          <div class="user-info">
            <span class="username">{{ users.find(user => user.id === message.fromUserId)?.nickname }}</span>
          </div>
          <div class="notification-text">{{ message.text }}</div>
          <div class="notification-time">{{ message.time }}</div>
        </div>

        <!-- 关联内容缩略图 -->
        <div class="content-thumbnail">
          <img :src="blogs.find(blog => blog.id === message.itemId)?.img" :alt="blogs.find(blog => blog.id === message.itemId)?.name">
        </div>
      </div>

      <!-- 结束标识 -->
      <div class="end-marker">
        - THE END -
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.content-container {
  width: 100%;
}

.header-title {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fff;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: #333;
  }
}

.notification-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background-color: #fff;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;

  &:hover {
    background-color: #fafafa;
    margin: 0 -20px;
    padding: 16px 20px;
    border-radius: 8px;
  }
}

.user-avatar {
  flex-shrink: 0;

  img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.notification-content {
  flex: 1;
  min-width: 0;

  .user-info {
    margin-bottom: 4px;

    .username {
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

  }

  .notification-text {
    color: #666;
    font-size: 14px;
    margin-bottom: 4px;
    line-height: 1.4;
  }

  .notification-time {
    color: #999;
    font-size: 12px;
  }
}

.content-thumbnail {
  flex-shrink: 0;

  img {
    width: 60px;
    height: 60px;
    border-radius: 6px;
    object-fit: cover;
    border: 1px solid #f0f0f0;
    cursor: pointer;
  }
}

.end-marker {
  text-align: center;
  color: #ccc;
  font-size: 12px;
  margin: 40px 0;
  padding: 20px 0;
}
</style>