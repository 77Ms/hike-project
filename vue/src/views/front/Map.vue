<template>
  <div class="map-container">
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入地点名称"
        clearable
        @keyup.enter="searchPlace"
      >
        <template #append>
          <el-button @click="searchPlace" type="primary">搜索</el-button>
        </template>
      </el-input>
    </div>
    <div v-if="errorMessage" class="error-message">
      <el-alert
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      />
    </div>
    <div id="map-container" class="map"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const searchKeyword = ref('')
const errorMessage = ref('')
let map = null
let placeSearch = null

const initMap = async () => {
  try {
    await AMapLoader.load({
      key: '16b1799df8664178e30e99f7d5304892', // API密钥
      version: '2.0',
      plugins: ['AMap.PlaceSearch'],

    })

    map = new AMap.Map('map-container', {
      zoom: 11,
      center: [110.289729, 25.279734] // 桂林
    })
    
    placeSearch = new AMap.PlaceSearch({
      pageSize: 5,
      pageIndex: 1,
      map: map,
    })

    const marker = new AMap.Marker({
      position: [110.289729, 25.279734],
      title: '默认位置'
    })
    marker.setMap(map)
    
  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}
window._AMapSecurityConfig={
  securityJsCode:'204bdeff0d0ab790bd6224d625d1351d'
}

const searchPlace = () => {
  if (!searchKeyword.value) return
  
  errorMessage.value = ''
  
  if (placeSearch) {
    placeSearch.search(searchKeyword.value, (status, result) => {
      if (status === 'complete' && result.info === 'OK') {
        console.log('搜索结果:', result)
      } else {
        console.error('搜索失败:', result)
        if (result === 'INVALID_USER_SCODE') {
          errorMessage.value = '高德地图API密钥无效，请在高德开放平台申请并配置正确的密钥'
        } else if (result === 'NO_PERMISSION') {
          errorMessage.value = '无权限使用该服务，请检查API密钥的权限设置'
        } else {
          errorMessage.value = `搜索失败: ${result}`
        }
      }
    })
  }
}

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
  }
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.search-box {
  margin-top: 10px;
  border-radius: 10px;
  padding: 20px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 100;
}

.error-message {
  margin: 10px 20px;
  z-index: 100;
}

.map {
  flex: 1;
  width: 100%;
  height: calc(100vh - 80px);
}
</style>