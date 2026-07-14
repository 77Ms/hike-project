<script setup>
import {reactive, ref, onMounted, computed, nextTick} from 'vue'
import {Star, StarFilled} from '@element-plus/icons-vue'
import request from '../../utils/request'
import {ElMessage} from "element-plus";

import {useRoute,useRouter} from "vue-router";

const router = useRouter()
const route = useRoute()
const keyword = route.query.keyword

// 表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// 搜索和筛选条件
const searchForm = reactive({
  keyword: keyword,
  typeId: 0
})

const loadBlog = (isLoadMore) => {
  const currentPageNum = isLoadMore ? pageNum.value + 1 : 1

  request.get("/blog/front/page", {
    params: {
      pageNum: currentPageNum,
      pageSize: pageSize.value,
      typeId: searchForm.typeId,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (res.code === '200') {
      if (isLoadMore) {
        tableData.value = tableData.value.concat(res.data.records);
        pageNum.value = currentPageNum;
      } else {
        tableData.value = res.data.records;
        pageNum.value = 1;
        waterfallRef.value.init()
      }
      total.value = res.data.total
    }
  })
}

const load = () => {
  loadBlog(false)
}

const loadMore = () => {
  loadBlog(true)
}

const users = ref([])
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data
  })
}

onMounted(() => {
  getWaterfallContainerWidth()
  load()
  loadUser()
})

// 瀑布流组件的引用
const waterfallRef = ref(null)

// 通过ref引用获取dom元素
const waterfallContainerRef = ref(null)
const waterfallContainerWidth = ref(0)

// 获取dom元素宽度
const getWaterfallContainerWidth = () => {
  waterfallContainerWidth.value = waterfallContainerRef.value.clientWidth
}

// 定义列数和博客宽度
const col = ref(4)
const blogWidth = ref(250)

// 计算间距
const gutterWidth = computed(() => {
  const totalBlogWidth = col.value * blogWidth.value
  return Math.floor((waterfallContainerWidth.value - totalBlogWidth) / col.value)
})

// Dialog相关状态
const blog = ref({})
const blogVisible = ref(false)
const dialogStyle = ref({})
const originalPosition = ref({})
const isClosing = ref(false) // 新增：标记是否正在关闭

const showBlog = (item, event) => {

  blog.value = item
  commentItemId.value = item.id
  loadComment()

  checkFollow(blog.value.userId)

  const clickedElement = event.currentTarget
  const rect = clickedElement.getBoundingClientRect()

  // 保存原始位置
  originalPosition.value = {
    top: rect.top,
    left: rect.left,
    width: rect.width,
    height: rect.height
  }

  // 设置初始样式 - 与点击元素完全重合
  dialogStyle.value = {
    position: 'fixed',
    top: `${originalPosition.value.top}px`,
    left: `${originalPosition.value.left}px`,
    width: `${originalPosition.value.width}px`,
    height: `${originalPosition.value.height}px`,
    opacity: '0',
    zIndex: '1800',
    borderRadius: '30px',
  }

  blogVisible.value = true
  isClosing.value = false // 重置关闭状态

  nextTick(() => {
    // 强制重绘
    requestAnimationFrame(() => {
      // 计算目标位置和尺寸
      const targetWidth = Math.min(1200, window.innerWidth * 0.9)
      const targetHeight = Math.min(800, window.innerHeight * 0.9)
      const targetLeft = (window.innerWidth - targetWidth) / 2
      const targetTop = (window.innerHeight - targetHeight) / 2

      // 开始动画到中心位置
      dialogStyle.value = {
        position: 'fixed',
        top: `${targetTop}px`,
        left: `${targetLeft}px`,
        width: `${targetWidth}px`,
        height: `${targetHeight}px`,
        transform: 'scale(1.1)',
        opacity: '1',
        zIndex: '1800',
        borderRadius: '30px',
        transition: 'all 0.5s ease',
      }

    })
  })
}

const closeBlog = () => {

  isClosing.value = true // 设置关闭状态

  // 动画回到原始位置
  dialogStyle.value = {
    position: 'fixed',
    top: `${originalPosition.value.top}px`,
    left: `${originalPosition.value.left}px`,
    width: `${originalPosition.value.width}px`,
    height: `${originalPosition.value.height}px`,
    opacity: '1',
    zIndex: '1800',
    borderRadius: '30px',
    transition: 'all 0.5s ease',
  }

  // 动画完成后隐藏dialog
  setTimeout(() => {
    blogVisible.value = false
    isClosing.value = false
  }, 500)
}

const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})

const commentItemId = ref(0);
const replyVisible = ref(false);
const comments = ref([]);
const commentForm = ref({});

const loadComment = () => {
  request.get("/comment/tree/" + commentItemId.value).then(res => {
    comments.value = res.data;
  });
};

const saveComment = () => {
  if (!account.value.id) {
    ElMessage.warning("请登录后操作");
    return;
  }
  commentForm.value.itemId = commentItemId;
  if (commentForm.value.contentReply) {
    commentForm.value.content = commentForm.value.contentReply;
  }
  request.post("/comment", commentForm.value).then(res => {
    if (res.code === '200') {
      ElMessage.success('评论成功')
      commentForm.value = {};
      loadComment();
    } else {
      ElMessage.error('评论失败')
    }
  })
};

const delComment = (id) => {
  request.delete("/comment/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功");
      loadComment();
    } else {
      ElMessage.error("删除失败");
    }
  });
};

const handleReply = (pid) => {
  commentForm.value = {pid: pid};
  replyVisible.value = true;
};

const cancelReply = () => {
  commentForm.value = {pid: ''};
  replyVisible.value = false;
};

const isFollowed = ref(false)

const checkFollow = (userId) => {
  request.get('/follow/check/' + userId).then(res => {
    if (res.code === '200') {
      isFollowed.value = true
    }else {
      isFollowed.value = false
    }
  })
}

const follow = (userId) => {
  let data = {
    itemId: userId,
  }
  request.post("/follow", data).then(res => {
    if (res.code === '200') {
      isFollowed.value = true
      ElMessage.success("关注成功");
    } else {
      isFollowed.value = false
      ElMessage.error(res.msg);
    }
  })
};

// 收藏功能
const collect = (itemId) => {
  let data = {
    itemId: itemId,
  }
  request.post("/collect", data).then(res => {
    if (res.code === '200') {
      blog.value.isCollected = true
      blog.value.count++
      ElMessage.success("收藏成功");
    } else {
      blog.value.isCollected = false
      blog.value.count--
      ElMessage.error(res.msg);
    }
  })
};
</script>

<template>
  <!-- 搜索关键词显示区域 -->
  <div class="search-keyword-display">
    <h2 class="search-title">搜索结果：{{ keyword || '无' }}</h2>
    <p class="search-subtitle">找到 {{ total }} 个相关内容</p>
  </div>

  <div ref="waterfallContainerRef">
    <waterfall :data="tableData" :col="col" :width="blogWidth" :gutterWidth="gutterWidth" :loadDistance="30"
               @loadmore="loadMore" ref="waterfallRef">
      <div class="cell-item" v-for="blog in tableData" :key="blog.id" @click="showBlog(blog,$event)">
        <div class="image-container">
          <img :src="blog.img" alt="加载错误"/>
          <div class="image-overlay"></div>
        </div>
        <div class="item-body">
          <div class="item-desc">{{ blog.name }}</div>
          <div class="item-footer">
            <div class="footer-left">
              <img class="item-img" :src="users.find(user=>user.id===blog.userId)?.avatarUrl" alt="User Avatar"/>
              <div class="name">{{ users.find(user => user.id === blog.userId)?.nickname }}</div>
            </div>
            <div class="footer-right">
              <el-icon v-if="blog.isCollected">
                <StarFilled/>
              </el-icon>
              <el-icon v-else>
                <Star/>
              </el-icon>
              <span>{{ blog.count || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </waterfall>
  </div>

  <!-- 背景遮罩 -->
  <div v-if="blogVisible" class="dialog-overlay" @click="closeBlog"></div>

  <!-- 自定义Dialog -->
  <div v-if="blogVisible" class="custom-dialog-container" :style="dialogStyle">
    <!-- 关闭按钮在左上角，关闭时隐藏 -->
    <button v-show="!isClosing" @click="closeBlog" class="close-button">
      x
    </button>

    <div class="custom-dialog-content">
      <div class="img-video-container" :class="{ 'closing-animation': isClosing }">
        <video controls autoplay :src="blog.video" class="video-player" v-if="blog.category==='视频'"></video>
        <el-image :src="blog.img" class="img" :preview-src-list="[blog.img]" v-else></el-image>
      </div>

      <!-- 右侧内容在关闭时隐藏 -->
      <div class="blog-container" :class="{ 'closing-hide': isClosing }">
        <!-- 右侧内容区域，使用滚动条 -->
        <div class="content-scroll-area">
          <!-- 用户信息和关注按钮 -->
          <div class="user-info">
            <img :src="users.find(user=>user.id===blog.userId)?.avatarUrl" alt="User Avatar" class="user-avatar">
            <span class="user-name"  @click="router.push('/front/user?id='+blog.userId)">{{ users.find(user => user.id === blog.userId)?.nickname }}</span>
            <button
                class="follow-button"
                :class="{ 'follow-button--followed': isFollowed }"
                @click="follow(blog.userId)"
            >
              {{ isFollowed ? '已关注' : '关注' }}
            </button>
          </div>

          <div class="blog-content">
            <h2 class="blog-title">{{ blog.name }}</h2>
            <p class="blog-description" v-html="blog.content"></p>
            <span class="blog-time">{{ blog.time }}</span>
          </div>

          <el-divider></el-divider>

          <!-- 评论区域 -->
          <div class="comment-section">
            <div class="comment-header">
              <span class="comment-count">共 {{ comments.length }} 条评论</span>
            </div>

            <div class="comment-list">
              <div v-for="item in comments" :key="item.id" class="comment-thread">
                <div class="comment-item">
                  <div class="comment-avatar">
                    <el-image :src="item.avatarUrl"></el-image>
                  </div>
                  <div class="comment-content">
                    <div class="comment-user">{{ item.nickname }}</div>
                    <div class="comment-text">{{ item.content }}</div>
                    <div class="comment-meta">
                      <span class="comment-time">{{ item.time }}</span>
                      <el-button link @click="handleReply(item.id)" class="comment-reply-btn">回复</el-button>
                      <el-button
                          link
                          @click="delComment(item.id)"
                          v-if="account.id === item.userId || account.role === 'ROLE_ADMIN'"
                          class="comment-delete-btn"
                      >
                        删除
                      </el-button>
                    </div>
                    <div class="comment-reply" v-if="commentForm.pid === item.id && replyVisible">
                      <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..."
                                size="small"></el-input>
                      <div class="reply-actions">
                        <el-button size="small" type="primary" @click="saveComment">发布</el-button>
                        <el-button size="small" @click="cancelReply">取消</el-button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 子评论 -->
                <template v-if="item.children?.length">
                  <div v-for="subItem in item.children" class="comment-item comment-sub-item" :key="subItem.id">
                    <div class="comment-avatar">
                      <el-image :src="subItem.avatarUrl"/>
                    </div>
                    <div class="comment-content">
                      <div class="comment-user">
                        {{ subItem.nickname }}
                        <span v-if="subItem.pid" class="reply-target">回复 @{{ subItem.pnickname }}</span>
                      </div>
                      <div class="comment-text">{{ subItem.content }}</div>
                      <div class="comment-meta">
                        <span class="comment-time">{{ subItem.time }}</span>
                        <el-button link @click="handleReply(subItem.id)" class="comment-reply-btn">回复</el-button>
                        <el-button
                            link
                            @click="delComment(subItem.id)"
                            v-if="account.id === subItem.userId || account.role === 'ROLE_ADMIN'"
                            class="comment-delete-btn"
                        >
                          删除
                        </el-button>
                      </div>
                      <div class="comment-reply" v-if="commentForm.pid === subItem.id && replyVisible">
                        <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..."
                                  size="small"></el-input>
                        <div class="reply-actions">
                          <el-button size="small" type="primary" @click="saveComment">发布</el-button>
                          <el-button size="small" @click="cancelReply">取消</el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>

            <!-- 底部留白，为固定评论框留出空间 -->
            <div class="comments-bottom-spacer"></div>
          </div>
        </div>

        <!-- 固定在底部的互动区域 -->
        <div class="interaction-footer">
          <div class="interaction-stats">
            <span class="stat-item" @click="collect(blog.id)">
              <el-icon v-if="blog.isCollected"><StarFilled/></el-icon>
              <el-icon v-else><Star/></el-icon>
              <span>{{ blog.count || 0 }}</span>
            </span>
          </div>

          <!-- 评论输入框 -->
          <div class="comment-input-wrapper">
            <el-input
                class="comment-input"
                v-model="commentForm.content"
                placeholder="说点什么..."
                size="small"
            />
            <el-button
                :disabled="!commentForm.content"
                class="comment-send-btn"
                type="primary"
                size="small"
                @click="saveComment"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 搜索关键词显示样式 */
.search-keyword-display {
  padding: 20px 0;
  margin-bottom: 20px;
  text-align: center;

  .search-title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  .search-subtitle {
    font-size: 14px;
    color: #666;
    margin: 0;
  }
}

.cell-item {
  width: 100%;
  height: auto;
  margin-bottom: 6px;
  background: #ffffff;
  border-radius: 6px;
  overflow: hidden;
  box-sizing: border-box;
  cursor: pointer;

  .image-container {
    position: relative;
    overflow: hidden;
    border-radius: 20px;

    img {
      width: 100%;
      height: auto;
      display: block;
      transition: none;
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.3);
      opacity: 0;
      transition: opacity 0.3s ease;
      pointer-events: none;
    }
  }

  &:hover .image-overlay {
    opacity: 1;
  }

  .item-body {
    margin: 9px;

    .item-desc {
      text-align: left;
      font-family: Roboto;
      font-style: normal;
      font-weight: normal;
      font-size: 16px;
      line-height: 16px;
      color: #000000;
      margin-left: 5px;
    }

    .item-footer {
      display: flex;
      justify-content: space-between;

      .footer-left {
        display: flex;
        align-items: center;
        font-family: SF Pro Display;
        font-style: normal;
        font-weight: normal;
        font-size: 12px;
        line-height: 14px;
        margin-top: 7px;
        color: rgba(0, 0, 0, 0.6);

        .item-img {
          border-radius: 50%;
          width: 22px;
          height: 22px;
          margin-right: 4px;
        }
      }

      .footer-right {
        font-size: 14px;
        display: flex;
        align-items: center;
      }
    }
  }
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 1800;
}

.custom-dialog-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  will-change: transform, opacity;

  .close-button {
    position: absolute;
    top: 15px;
    left: 15px;
    background: rgba(0, 0, 0, 0.6);
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    font-size: 18px;
    cursor: pointer;
    z-index: 10;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.8);
    }
  }

  .custom-dialog-content {
    display: flex;
    width: 100%;
    height: 100%;

    .img-video-container {
      flex: 1;
      background-color: #000;
      display: flex;
      align-items: center;
      justify-content: center;

      &.closing-animation {
        flex: 1;
        width: 100%;
      }

      .video-player {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      .img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    .blog-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      position: relative;
      background: #fff;
      transition: all 0.5s ease;

      &.closing-hide {
        width: 0;
        flex: 0;
      }
    }
  }
}

.content-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
  padding-bottom: 120px; // 为固定底部区域留出空间

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;

    &:hover {
      background: #a8a8a8;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  padding: 20px 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;

  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 12px;
  }

  .user-name {
    cursor: pointer;
    font-weight: 600;
    font-size: 16px;
    color: #333;
    flex: 1;
  }

  .follow-button {
    background-color: #ff2442;
    color: #fff;
    border: none;
    padding: 8px 20px;
    border-radius: 20px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background-color: #e01e3c;
    }

    &--followed {
      background-color: #f0f0f0;
      color: #666;
      border: 1px solid #d9d9d9;

      &:hover {
        background-color: #e8e8e8;
      }
    }
  }
}

.blog-content {
  .blog-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
    line-height: 1.4;
  }

  .blog-description {
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    margin-bottom: 12px;
    word-break: break-word;
  }

  .blog-time {
    font-size: 12px;
    color: #999;
    display: block;
    margin-bottom: 16px;
  }
}

.comment-section {
  margin-top: 20px;
}

.comment-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;

  .comment-count {
    font-size: 14px;
    color: #666;
  }
}

.comment-list {
  .comment-thread {
    margin-bottom: 16px;
  }
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;

  &.comment-sub-item {
    margin-left: 40px;
    padding: 12px;
    background-color: #f8f8f8;
    border-radius: 8px;
  }

  .comment-avatar {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .comment-content {
    flex: 1;
    min-width: 0;
  }

  .comment-user {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;

    .reply-target {
      color: #1890ff;
      font-weight: normal;
    }
  }

  .comment-text {
    font-size: 14px;
    line-height: 1.4;
    color: #333;
    margin-bottom: 8px;
    word-break: break-word;
  }

  .comment-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #999;

    .comment-time {
      color: #999;
    }

    .comment-reply-btn,
    .comment-delete-btn {
      font-size: 12px;
      color: #666;
      padding: 0;
      height: auto;

      &:hover {
        color: #1890ff;
      }
    }

    .comment-delete-btn:hover {
      color: #ff4d4f;
    }
  }

  .comment-reply {
    margin-top: 12px;
    padding: 12px;
    background-color: #f5f5f5;
    border-radius: 6px;

    .reply-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      margin-top: 8px;
    }
  }
}

.comments-bottom-spacer {
  height: 20px;
}

.interaction-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;

  .interaction-stats {
    display: flex;
    gap: 20px;
    margin-bottom: 12px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;
      color: #666;
      font-size: 14px;
      transition: color 0.2s ease;

      &:hover {
        color: #ff2442;
      }
    }
  }

  .comment-input-wrapper {
    display: flex;
    gap: 8px;
    align-items: flex-end;

    .comment-input {
      flex: 1;

      :deep(.el-input__wrapper) {
        border-radius: 20px;
        background-color: #f8f8f8;
        border: 1px solid #e0e0e0;

        &.is-focus {
          border-color: #1890ff;
          background-color: #fff;
        }
      }
    }

    .comment-send-btn {
      border-radius: 16px;
      padding: 8px 16px;
    }
  }
}
</style>