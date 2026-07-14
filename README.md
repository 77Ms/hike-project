# 🏔️ 行山记

一个基于 **Spring Boot + Vue 3** 的户外游记分享平台，支持用户发布游记、评论互动、收藏关注、地图展示等功能。

## 技术栈

| 后端 | 前端 |
|------|------|
| Spring Boot 3.5.9 | Vue 3 (Vite) |
| MyBatis-Plus 3.5.9 | Element Plus |
| MySQL | ECharts |
| JWT 认证 | 高德地图 API |
| Hutool 工具库 | Axios |
| Lombok | Vue Router |

## 功能模块

### 前台功能
- 🔐 用户注册 / 登录
- 📝 发布游记（富文本编辑器）
- 💬 评论互动（树形结构）
- ⭐ 收藏 / 关注
- 🔍 搜索游记
- 🗺️ 地图展示
- 👤 个人信息管理

### 后台管理
- 📊 后台首页统计
- 👥 用户管理
- 🔑 管理员管理
- 📂 分类管理
- 📄 内容管理
- 💬 评论管理
- ⭐ 收藏管理
- 👀 关注管理
- ✉️ 消息管理
- 🔞 敏感词管理

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20+
- Maven 3.6+
- MySQL 8.0+

### 1. 数据库

```sql
CREATE DATABASE springboot DEFAULT CHARACTER SET utf8mb4;
```

> 脚本位于项目根目录或自行根据实体类建表。

### 2. 后端启动

```bash
# 修改数据库配置
# 编辑 src/main/resources/application.yaml
# 将 ip 改为你的数据库地址，username/password 改为你的数据库账号

# 启动
mvn spring-boot:run
```

后端默认运行在 `http://localhost:9090`

### 3. 前端启动

```bash
cd vue

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`

### 4. 生产构建

```bash
# 构建前端
cd vue
npm run build

# 构建后端
cd ..
mvn clean package -DskipTests
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

## 项目结构

```
springboot/
├── src/main/java/com/example/springboot/
│   ├── common/          # 通用工具类（常量、统一返回结果）
│   ├── config/          # 配置类（跨域、拦截器、JWT、MyBatis-Plus）
│   ├── controller/      # 控制器层
│   ├── entity/          # 实体类
│   ├── exception/       # 全局异常处理
│   ├── mapper/          # MyBatis-Plus Mapper
│   ├── service/         # 服务层
│   └── utils/           # 工具类（DFA敏感词过滤、文本分析、Token）
├── src/main/resources/
│   ├── application.yaml # 配置文件
│   └── mapper/          # MyBatis XML 映射文件
├── vue/                 # 前端项目
│   └── src/
│       ├── views/       # 页面组件
│       │   ├── front/   # 前台页面
│       │   └── back/    # 后台管理页面
│       ├── router/      # 路由配置
│       └── utils/       # 工具函数
└── pom.xml
```

## API 接口

| 前缀 | 说明 |
|------|------|
| `/web/` | 登录、注册、文件上传下载 |
| `/user/` | 用户管理 |
| `/admin/` | 管理员管理 |
| `/blog/` | 游记内容 |
| `/type/` | 分类管理 |
| `/comment/` | 评论管理 |
| `/collect/` | 收藏管理 |
| `/follow/` | 关注管理 |
| `/message/` | 消息管理 |
| `/word/` | 敏感词管理 |

## 许可证

MIT