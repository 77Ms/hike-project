<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { projectName } from '../../config/config.default'
import { User, Lock, SwitchButton, House, VideoCamera, Message, PictureRounded, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 路由实例
const router = useRouter()
const route = useRoute()

// 用户信息
const account = ref(
    localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {}
)

// 当前激活的菜单项
const activeMenu = computed(() => route.path)

// 侧边栏菜单项
const sidebarMenus = ref([
  {
    name: '发现',
    icon: 'House',
    path: '/front/home',
    active: true
  },
  {
    name: '发布',
    icon: 'VideoCamera',
    path: '/front/publish',
    active: false
  },
  {
    name: '通知',
    icon: 'Message',
    path: '/front/message',
    active: false
  },
  {
    name: '地图',
    icon: 'Location',
    path: '/front/map',
    active: false
  },
  {
    name: '我',
    icon: 'User',
    path: '/front/user?id='+account.value.id,
    active: false
  }
])

// 当前激活的侧边栏菜单
const activeSidebarMenu = ref('/front/home')

// 切换侧边栏菜单
const switchSidebarMenu = (menu) => {
  activeSidebarMenu.value = menu.path
  sidebarMenus.value.forEach(item => {
    item.active = item.path === menu.path
  })
  router.push(menu.path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('account')
  ElMessage.success('退出成功')
  router.push('/login')
}

const handleUpdateAccount = (updatedAccount) => {
  // 更新父组件中的用户信息
  account.value = updatedAccount
}

const keyword = ref('')

const search = ()=>{
  location.href = '/front/search?keyword=' + keyword.value || ''
}

const clearSearch =()=> {
  location.href = '/front/search?keyword=' + ''
}

</script>

<template>

  <el-backtop :right="50" :bottom="50" />

  <div class="front-container">
    <!-- 顶部导航栏 -->
    <header class="header-nav">
      <div class="header-left-warp">
        <div class="logo-warp" @click="router.push('/front/home')">
          <div class="logo">
            <img src="../../config/logo.svg" alt="Logo" />
          </div>
          <div class="logo-text">{{ projectName }}</div>
        </div>

        <div class="header-navs">
          <el-menu
              router
              :default-active="activeMenu"
              mode="horizontal"
              :ellipsis="false"
          >
            <!--前台路由-->
            <!--            <el-menu-item index="/front/home">前台首页</el-menu-item>-->
            <!--前台路由-->
          </el-menu>
        </div>
      </div>

      <div style="display: flex;justify-content: space-around;">
        <el-input size="large" v-model="keyword" @clear="clearSearch" clearable placeholder="搜索" style="width:400px"></el-input>
        <el-button size="large" type="danger" @click="search" style="margin-left: 5px">搜索</el-button>
      </div>

      <div class="user-warp">
        <!-- 未登录状态显示登录注册按钮 -->
        <template v-if="!account.id">
          <div class="btn-login">
            <el-button @click="router.push('/login')">登录</el-button>
          </div>
          <div class="btn-login" style="margin-left: 10px">
            <el-button @click="router.push('/register')">注册</el-button>
          </div>
        </template>

        <!-- 已登录状态显示用户头像和下拉菜单 -->
        <el-dropdown v-else class="custom-dropdown">
          <div class="user-avatar">
            <img :src="account.avatarUrl" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>{{ account.nickname }}</el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/back/blog" class="dropdown-link">
                  <el-icon><PictureRounded /></el-icon>
                  <span>管理作品</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/person" class="dropdown-link">
                  <el-icon><User /></el-icon>
                  <span>个人信息</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/password" class="dropdown-link">
                  <el-icon><Lock /></el-icon>
                  <span>修改密码</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <div @click="logout" class="dropdown-link">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区域 -->
    <div class="main-content">
      <div class="main-left">
        <!-- 侧边栏导航 -->
        <div class="sidebar-nav">
          <div class="sidebar-menu">
            <!-- 主要菜单项 -->
            <div v-for="menu in sidebarMenus" class="sidebar-menu-item" :class="{ 'active': activeSidebarMenu === menu.path }" @click="switchSidebarMenu(menu)">
              <div class="menu-icon">
                <el-icon v-if="menu.icon === 'House'" style="margin-left: 6px;"><House /></el-icon>
                <el-icon v-else-if="menu.icon === 'VideoCamera'" style="margin-left: 2px;"><VideoCamera /></el-icon>
                <el-icon v-else-if="menu.icon === 'Message'" style="margin-left: 2px;"><Message /></el-icon>
                <el-icon v-else-if="menu.icon === 'Location'" style="margin-left: 2px;"><Location /></el-icon>
                <el-avatar v-else :src="account.avatarUrl" :size="24"></el-avatar>
              </div>
              <span class="menu-text">{{ menu.name }}</span>
            </div>
          </div>

        </div>
      </div>
      <div class="main-right">
        <router-view @update-account="handleUpdateAccount"></router-view>
      </div>
    </div>

    <!-- 页脚 -->
    <footer class="front-footer">
      <p>© {{ new Date().getFullYear() }} {{ projectName }}. 保留所有权利</p>
    </footer>
  </div>
</template>

<style lang="scss" scoped>

/*定义前台头部 背景 主题色*/
$front-back-color: #fff;

/*定义前台头部 字体 主题色*/
$front-font-color: #d54941;

.front-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  //color: #3a76c4;
}

.header-nav {
  z-index: 1800;
  position: sticky;
  top: 0;
  height: 70px;
  background-color: $front-back-color;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.1);
  overflow: visible;

  .header-left-warp {
    display: flex;
    align-items: center;
    height: 100%;

    .logo-warp {
      cursor: pointer;
      display: flex;
      align-items: center;
      margin-left: 20px;

      .logo {
        width: 30px;
        height: 30px;
        margin-right: 10px;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .logo-text {
        font-size: 22px;
        font-weight: 500;
        color: $front-font-color;
      }

    }

    .header-navs{
      margin-left: 80px;
      height: 100%;

      .el-menu {
        background-color: $front-back-color !important;
        border: none !important;
        height: 70px !important;
      }

      .el-menu-item {
        height: 70px !important;
        line-height: 70px !important;
        border: none !important;
      }

      .el-menu-item:hover {
        color: $front-font-color !important;
        background-color: transparent !important;
      }

      .el-menu-item.is-active {
        color: $front-font-color !important;
        background-color: transparent !important;
        border: none !important;
      }

    }

  }

  .user-warp {
    display: flex;
    align-items: center;
    margin-right: 20px;
    height: 100%; /* 确保高度与父元素一致 */

    .btn-login {
      margin-top: 0;
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      border: 1px solid $front-font-color;
      padding: 2px;
      cursor: pointer;
      outline: none !important;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
      }

    }

    .dropdown-link {
      display: flex;
      align-items: center;
      color: inherit;
      text-decoration: none;

      .el-icon {
        margin-right: 8px;
      }
    }

  }
}

.main-content {
  flex: 1;
  background-color: #fff;
  display: flex;
  gap: 30px;

  .main-left{
    width: 20%;
    padding: 20px 0;

    .sidebar-nav {
      position: sticky;
      top: 90px;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 90px);

      .sidebar-menu {
        flex: 1;
        padding: 0 20px;
        margin-left: 100px;
      }

      .sidebar-menu-item {
        display: flex;
        align-items: center;
        padding: 16px 12px;
        margin-bottom: 8px;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        color: #333;
        font-size: 16px;
        font-weight: 500;

        &:hover {
          background-color: #f8f8f8;
        }

        &.active {
          background-color: #fff2f0;
          color: $front-font-color;
          border-radius: 20px;

          .menu-icon {
            color: $front-font-color;
          }
        }

        .menu-icon {
          width: 24px;
          height: 24px;
          margin-right: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #666;
          font-size: 24px;

          .el-icon {
            font-size: 24px;
          }
        }

        .menu-text {
          flex: 1;
          font-size: 16px;
          font-weight: 500;
        }
      }
    }
  }

  .main-right{
    flex: 1;
  }

}

.front-footer {
  padding: 16px 24px;
  text-align: center;
  background-color: #fff;
  color: #666;
  font-size: 12px;
  border-top: 1px solid #eee;
}

</style>