import { createApp } from 'vue'
import App from './App.vue'
const app = createApp(App)
import router from './router'
app.use(router)
//全局样式
import './style/index.scss'

//引入element-plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
//引入element-plus中文库
import zhCn from 'element-plus/es/locale/lang/zh-cn'
app.use(ElementPlus, {locale: zhCn,size: 'small'})

//引入瀑布流组件
import waterfall from 'vue-waterfall2';
app.use(waterfall);

app.mount('#app')
