<script setup>
import { ref, reactive, shallowRef } from 'vue'
import { Search, Plus, Delete, Edit, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { serverHost } from '../../../config/config.default'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import axios from "axios"


// 表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 搜索条件
const searchForm = reactive({
  keyword: '',
})

// 表单数据
const form = ref({})
const dialogFormVisible = ref(false)
const multipleSelection = ref([])

// 用户信息
const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})

// 加载数据
const load = () => {
  request.get("/blog/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  })
}
load()

// 保存
const save = () => {

  form.value.content = htmlContent.value;

  request.post("/blog", form.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("保存成功")
      dialogFormVisible.value = false
      load()
    } else {
      ElMessage.error("保存失败")
    }
  })
}

// 添加
const handleAdd = () => {

  htmlContent.value = '';

  form.value = {}
  dialogFormVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))

  htmlContent.value = form.value.content || '';

  dialogFormVisible.value = true
}

// 删除
const del = (id) => {
  request.delete("/blog/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功")
      load()
    } else {
      ElMessage.error("删除失败")
    }
  })
}

// 批量删除
const delBatch = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  const ids = multipleSelection.value.map(v => v.id)
  request.post("/blog/del/batch", ids).then(res => {
    if (res.code === '200') {
      ElMessage.success("批量删除成功")
      load()
    } else {
      ElMessage.error("批量删除失败")
    }
  })
}

// 重置搜索
const reset = () => {
  searchForm.keyword = ""
  load()
}

// 表格选择变化
const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  load()
}

// 页码变化
const handleCurrentChange = (current) => {
  pageNum.value = current
  load()
}

// 确认删除
const confirmDelete = (id) => {
  ElMessageBox.confirm(
      '确定要删除这条数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        del(id)
      })
}

// 确认批量删除
const confirmBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  ElMessageBox.confirm(
      '确定要批量删除这些数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        delBatch()
      })
}

// 封面上传
const handleImgUploadSuccess = (res) => {
  form.value.img = res;
};

// 视频上传
const handleVideoUploadSuccess = (res) => {
  form.value.video = res;
};

// 内容富文本
const htmlContent = ref('');
const editorRefContent = shallowRef();

// 文件下载
const download = (url) => {
  window.open(url)
}

// 查看
const contentViewVisible = ref(false)
const currentViewContent = ref('')

const viewContent = (content) => {
  currentViewContent.value = content || ''
  contentViewVisible.value = true
}

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
  }).catch((error) => {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  })
}

// wangEditor 配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
    uploadVideo: {
      customUpload: async (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
  }
}

const types = ref([]);
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data;
  });
};
loadType();

const users = ref([]);
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data;
  });
};
loadUser();



</script>

<template>
  <div class="content-container">

    <!-- 搜索区域 -->
    <div class="header-section">
      <el-input v-model="searchForm.keyword" placeholder="请输入标题" class="filter-input" :prefix-icon="Search" clearable/>
      <el-button class="ml-10" plain type="primary" @click="load">搜索</el-button>
      <el-button plain type="info" @click="reset">重置</el-button>
    </div>

    <!-- 操作按钮区域 -->
    <div class="toolbar-section">
<!--      <el-button plain type="primary" @click="handleAdd" :icon="Plus">新增</el-button>-->
      <el-button plain type="danger" @click="confirmBatchDelete" :icon="Delete">批量删除</el-button>
    </div>

    <!-- 表格区域 -->
    <el-card>
      <el-table :data="tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="60" align="center"/>
        <el-table-column prop="id" label="ID" width="80" align="center"/>
        <el-table-column prop="name" label="标题"/>
        <el-table-column label="分类" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.typeId">{{ types.find(item => item.id === scope.row.typeId)?.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.userId">{{ users.find(item => item.id === scope.row.userId)?.nickname }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="发布时间" width="140" align="center"/>
        <el-table-column label="封面" width="120" align="center">
          <template #default="scope">
            <el-image style="width: 80px; height: 80px" :src="scope.row.img" :preview-src-list="[scope.row.img]" :preview-teleported=true></el-image>
          </template>
        </el-table-column>
        <el-table-column label="视频" width="100" align="center">
          <template #default="scope">
            <el-button type="primary" :disabled="!scope.row.video" @click="download(scope.row.video)">下载</el-button>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag>{{scope.row.category}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容" width="100" align="center">
          <template #default="scope">
            <el-button type="primary" @click="viewContent(scope.row.content)">查看</el-button>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-tooltip content="编辑" placement="top" :effect="'light'" v-if="account.role!=='ROLE_ADMIN'">
              <el-button circle type="primary" :icon="Edit" @click="handleEdit(scope.row)"/>
            </el-tooltip>
            <el-tooltip content="删除" placement="top" :effect="'light'">
              <el-button circle type="danger" :icon="Delete" @click="confirmDelete(scope.row.id)"/>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-section">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <el-dialog v-model="dialogFormVisible" :title="form.id ? '编辑' : '新增'" width="40%" center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.name" placeholder="请输入标题"/>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.typeId" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="item in types" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
<!--        <el-form-item label="用户">-->
<!--          <el-select v-model="form.userId" placeholder="请选择用户" style="width: 220px">-->
<!--            <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />-->
<!--          </el-select>-->
<!--        </el-form-item>-->
<!--        <el-form-item label="发布时间">-->
<!--          <el-date-picker v-model="form.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择日期时间"></el-date-picker>-->
<!--        </el-form-item>-->
        <el-form-item label="封面">
          <div class="upload-container">
            <el-avatar v-if="form.img" :src="form.img" :size="80" />
            <el-upload :action="`${serverHost}/web/upload`" :on-success="handleImgUploadSuccess" :show-file-list="false">
              <el-button type="primary" :icon="UploadFilled">{{ form.img ? '更换图片' : '上传图片' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="视频">
          <el-upload :action="`${serverHost}/web/upload`" :on-success="handleVideoUploadSuccess" :limit="1">
            <el-button type="primary" :icon="UploadFilled">{{ form.video ? '更换文件' : '上传文件' }}  </el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.category">
            <el-radio :value="'图片'">图片</el-radio>
            <el-radio :value="'视频'">视频</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <div style="border: 1px solid #ccc; z-index: 100;">
            <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRefContent" :defaultConfig="editorConfig" mode="default" />
            <Editor style="height: 300px; overflow-y: hidden;" v-model="htmlContent" :defaultConfig="editorConfig" mode="default" @onCreated="editorRefContent = $event" />
          </div>
        </el-form-item>

      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </div>
      </template>

    </el-dialog>

    <el-dialog v-model="contentViewVisible" title="详情" width="40%" center>
      <div v-html="currentViewContent"></div>
    </el-dialog>

  </div>
</template>

<style scoped>


</style>