<script setup>
import {ref, reactive, onMounted, nextTick, shallowRef} from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

import axios from 'axios'
import request from '../../utils/request'
import { serverHost } from '../../../config/config.default'

// 响应式数据
const form = reactive({
  name: '',
  typeId: '',
  content: '',
  img: '',
  video: '',
  category: ''
})

// 内容富文本
const htmlContent = ref('');
const editorRefContent = shallowRef();

const ruleFormRef = ref(null)
const videoRef = ref(null)
const canvasRef = ref(null)

// 新增：视频封面预览和提交状态
const videoCoverPreview = ref('') // 用于预览的封面
const isSubmitting = ref(false) // 提交状态

// 表单验证规则
const rules = reactive({
  name: [
    { required: true, message: '请输入标题', trigger: 'blur' }
  ],
  typeId: [
    { required: true, message: '请选择分类', trigger: 'blur' }
  ]
})

// 自定义上传方法
const customUpload = (file, insertFn) => {
  const formData = new FormData()
  formData.append('file', file)
  axios({
    url: `${serverHost}/web/upload`,
    method: 'post',
    data: formData,
    headers: {'Content-Type': 'multipart/form-data'},
  }).then(res => {
    insertFn(res.data)
  })
}

// wangEditor 配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
    uploadVideo: {
      customUpload: (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
  }
}

// 切换分类标签
const changeCategory = () => {
  form.img = ''
  form.video = ''
  videoCoverPreview.value = ''
}

// 重置表单
const resetForm = () => {
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  htmlContent.value = ''
  videoCoverPreview.value = ''
}

// 提交表单
const submitForm = () => {
  request.post('/blog', form).then(res => {
    if (res.code === '200') {
      ElMessage.success('投稿成功')
      resetForm()
    } else {
      ElMessage.error(res.msg || '投稿失败')
    }
    isSubmitting.value = false
  })
}

// 生成视频封面（仅用于预览）
const generateVideoCoverPreview = () => {
  const video = videoRef.value
  const canvas = canvasRef.value

  if (!video || !canvas) return

  const ctx = canvas.getContext('2d')
  video.crossOrigin = 'anonymous'

  video.onloadeddata = () => {
    video.currentTime = 0
  }

  video.onseeked = () => {
    canvas.width = video.videoWidth || video.clientWidth
    canvas.height = video.videoHeight || video.clientHeight
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    videoCoverPreview.value = canvas.toDataURL('image/png')
  }
}

const uploadVideoCoverAndSubmit = () => {

  // 将预览图转换为 Blob
  const imgSrcBase64 = videoCoverPreview.value
  const byteString = window.atob(imgSrcBase64.split(',')[1])
  const mimeString = imgSrcBase64.split(',')[0].split(':')[1].split(';')[0]
  const ab = new ArrayBuffer(byteString.length)
  const ia = new Uint8Array(ab)

  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i)
  }

  const blob = new Blob([ab], {type: mimeString})

  // 组装文件上传对象
  const formData = new FormData()
  formData.append('file', blob, 'cover.png')

  // 通过axios发送网络请求到后端上传接口
  axios({
    url: `${serverHost}/web/upload`,
    method: 'post',
    data: formData,
    headers: {'Content-Type': 'multipart/form-data'}
  }).then(res => {
    form.img = res.data
    submitForm()
  })
}

// 保存表单
const save = () => {
  ruleFormRef.value.validate((valid) => {
    if (!valid) return

    isSubmitting.value = true
    form.content = htmlContent.value

    if (form.category === '视频' && form.video && !form.img) {
      uploadVideoCoverAndSubmit()
    } else {
      submitForm()
    }
  })
}

// 图片上传成功回调
const handleImgUploadSuccess = (res) => {
  form.img = res
  form.category = '图片'
  videoCoverPreview.value = ''
}

// 视频上传成功回调
const handleVideoUploadSuccess = (res) => {
  form.video = res
  form.category = '视频'
  form.img = ''

  nextTick(() => {
    generateVideoCoverPreview()
  })
}

const types = ref([])
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data
  })
}

// 生命周期钩子
onMounted(() => {
  loadType()
})
</script>

<template>
  <div class="main-content">
    <el-card>
      <div class="title">创作服务平台</div>
      <el-form label-width="120px" size="small" style="width: 90%" :model="form" :rules="rules" ref="ruleFormRef">
        <el-tabs @tab-click="changeCategory">
          <el-tab-pane label="上传图片">
            <el-upload
                class="img-uploader"
                :action="`${serverHost}/web/upload`"
                :show-file-list="false"
                :on-success="handleImgUploadSuccess"
            >
              <img v-if="form.img" :src="form.img" class="img">
              <el-icon v-else class="img-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </el-tab-pane>

          <el-tab-pane label="上传视频">
            <el-upload
                class="img-uploader"
                :action="`${serverHost}/web/upload`"
                :show-file-list="false"
                :on-success="handleVideoUploadSuccess"
            >
              <img v-if="videoCoverPreview" :src="videoCoverPreview" class="img">
              <el-icon v-else class="img-uploader-icon"><Plus /></el-icon>
            </el-upload>

            <div v-if="form.video" class="video-preview">
              <video ref="videoRef" controls :src="form.video" class="video-player"></video>
              <canvas ref="canvasRef" style="display: none"></canvas>
            </div>
          </el-tab-pane>
        </el-tabs>

        <el-form-item prop="name" label="标题">
          <el-input v-model="form.name" autocomplete="off" placeholder="好的创作值得一个好名字！"></el-input>
        </el-form-item>

        <el-form-item prop="typeId" label="分类">
          <el-select v-model="form.typeId" placeholder="请选择分类">
            <el-option
                v-for="item in types"
                :key="item.id"
                :label="item.name"
                :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item prop="content" label="内容">
          <div style="border: 1px solid #ccc; z-index: 100;">
            <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRefContent" :defaultConfig="editorConfig" mode="default" />
            <Editor style="height: 300px; overflow-y: hidden;" v-model="htmlContent" :defaultConfig="editorConfig" mode="default" @onCreated="editorRefContent = $event" />
          </div>
        </el-form-item>
      </el-form>

      <div style="margin-top: 10px;display: flex;justify-content: space-around">
        <el-button type="success" size="large" @click="save" :loading="isSubmitting">
          {{ isSubmitting ? '提交中...' : '立即投稿' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.main-content {
  display: flex;
  background-color: #fff;
  padding: 30px 20px;
  flex-direction: column;

  .title {
    font-size: 25px;

    &::after {
      content: '';
      width: 146px;
      height: 4px;
      display: block;
      border-radius: 10px;
    }

    .info {
      background-color: #eee;
      font-size: 16px;
      margin-bottom: 50px;
      width: 95%;
      border-radius: 10px;
      padding: 25px 10px 20px;

      .time {
        margin-bottom: 10px;
      }
    }

    .btns-wrap {
      display: flex;
      justify-content: space-between;
      margin-top: 20px;
      align-items: center;

      .el-button {
        height: 40px;
        width: 50%;
        font-size: 14px;
        border-radius: 50px;
      }
    }
  }
}

.img-uploader {
  text-align: center;
  padding-bottom: 10px;
}

:deep(.img-uploader .el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

:deep(.img-uploader .el-upload:hover) {
  border-color: #409EFF;
}

.img-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 350px;
  height: 280px;
  line-height: 280px;
  text-align: center;
}

.img {
  width: 350px;
  height: 280px;
  display: block;
}

.video-preview {
  width: 100%;
  max-width: 600px;
  margin: 20px auto;
  display: flex;
  justify-content: center;

  .video-player {
    width: 100%;
    max-width: 500px;
    height: auto;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}
</style>