<script setup>

import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import * as echarts from 'echarts';

const userCount = ref(0)
const blogCount = ref(0)

const loadUserCount = ()=>{
  request.get('/user').then(res=>{
    userCount.value = res.data.length
  })
}
loadUserCount()

const loadBlogCount = ()=>{
  request.get('/blog').then(res=>{
    blogCount.value = res.data.length
  })
}
loadBlogCount()


onMounted(()=>{

  const chartDom = document.getElementById('main');
  const myChart = echarts.init(chartDom);
  const option = {
    title: {
      text: '不同分类作品数量饼图',
      subtext: '饼图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: 'Access From',
        type: 'pie',
        radius: '50%',
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };

  request.get("/blog/count").then(res=>{
    res.data.forEach(item=>{
      option.series[0].data.push(item)
    })
    option && myChart.setOption(option);
  })

})

</script>

<template>
  <div style="padding: 10px">

    <el-row :gutter="24">

      <el-col :span="12" style="margin-top: 20px;display: flex;justify-content: space-around">
        <el-card style="width: 100%;font-size: 20px">
          <span>用户数量:{{userCount}}</span>
        </el-card>
      </el-col>

      <el-col :span="12" style="margin-top: 20px;display: flex;justify-content: space-around">
        <el-card style="width: 100%;font-size: 20px">
          <span>博客数量:{{blogCount}}</span>
        </el-card>
      </el-col>

    </el-row>

    <el-row :gutter="24" style="margin-top: 50px">

      <el-col :span="24" style="margin-top: 20px;display: flex;justify-content: space-around">
        <div id="main" style="width: 100%; height: 600px"></div>
      </el-col>

    </el-row>

  </div>
</template>

<style scoped>

</style>