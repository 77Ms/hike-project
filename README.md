# 🏔️ 行山记 — 户外游记分享平台

一个基于 **Spring Boot + Vue 3** 的户外游记分享平台，支持用户发布游记、互动评论、收藏关注、地图足迹展示、AI 问答等功能。

## 技术栈

### 后端
| 技术 | 说明 |
|------|------|
| **Spring Boot 3.5.9** | 基础框架 |
| **MyBatis-Plus 3.5.9** | ORM 框架 |
| **MySQL 8.0+** | 关系型数据库 |
| **JWT** | 用户认证（Token 拦截） |
| **Hutool** | Java 工具库 |
| **Lombok** | 代码简化 |

### 前端
| 技术 | 说明 |
|------|------|
| **Vue 3 (Vite)** | 前端框架 |
| **Element Plus** | UI 组件库 |
| **Vue Router** | 前端路由 |
| **Axios** | HTTP 请求库 |
| **ECharts** | 数据可视化 |
| **高德地图 API** | 地图足迹展示 |

### 特色模块
| 技术 | 说明 |
|------|------|
| **AI 大模型（DeepSeek）** | AI 智能问答、知识库 RAG |
| **文本嵌入（Embedding）** | 知识库向量化与语义检索 |
| **DFA 算法** | 敏感词过滤 |
| **树形结构** | 评论嵌套展示 |

## 功能模块

### 前台功能（`vue/src/views/front/`）

| 功能 | 描述 |
|------|------|
| 🔐 **注册 / 登录** | 用户账号注册与 JWT 登录 |
| 🏠 **首页** | 游记列表展示、搜索 |
| 📝 **发布游记** | 富文本编辑器，支持图片/视频上传 |
| 💬 **评论互动** | 树形嵌套评论，支持回复 |
| ⭐ **收藏 / 关注** | 收藏游记、关注其他用户 |
| 🗺️ **地图足迹** | 高德地图展示用户足迹 |
| 🔍 **搜索游记** | 关键词检索游记内容 |
| 👤 **个人中心** | 个人信息编辑、密码修改、文章管理 |
| ✉️ **消息通知** | 互动消息提醒 |
| 🤖 **AI 助手** | 智能问答机器人（基于 DeepSeek + 知识库） |

### 后台管理（`vue/src/views/back/`）

| 功能 | 描述 |
|------|------|
| 📊 **数据统计** | ECharts 可视化仪表盘 |
| 👥 **用户管理** | 用户信息管理 |
| 🔑 **管理员管理** | 管理员账号管理 |
| 📂 **分类管理** | 游记分类管理 |
| 📄 **内容管理** | 游记内容审核与管理 |
| 💬 **评论管理** | 评论内容管理 |
| ⭐ **收藏管理** | 收藏数据管理 |
| 👀 **关注管理** | 用户关注关系管理 |
| ✉️ **消息管理** | 系统消息管理 |
| 🔞 **敏感词管理** | DFA 敏感词库维护 |
| 🤖 **AI 知识库** | 知识库文档同步管理 |

## 项目结构

```
springboot/
├── src/main/java/com/example/springboot/
│   ├── ai/                      # AI 模块（DeepSeek 大模型、Embedding、RAG）
│   │   ├── AIService.java       # AI 问答服务
│   │   ├── AiConfig.java        # AI 配置
│   │   ├── EmbeddingService.java# 文本向量化服务
│   │   ├── KnowledgeSyncService.java # 知识库同步
│   │   ├── LlmClient.java       # LLM 客户端
│   │   └── ...
│   ├── common/                   # 通用工具类
│   │   ├── Constants.java        # 常量定义
│   │   └── Result.java           # 统一返回结果
│   ├── config/                   # 配置类
│   │   ├── CorsConfig.java       # 跨域配置
│   │   ├── InterceptorConfig.java# 拦截器配置
│   │   ├── MybatisPlusConfig.java# MyBatis-Plus 配置
│   │   └── interceptor/
│   │       ├── AuthAccess.java   # 权限注解
│   │       └── JwtInterceptor.java# JWT 拦截器
│   ├── controller/               # 控制器层（11 个 REST 控制器）
│   ├── entity/                   # 实体类（12 个数据实体）
│   ├── exception/                # 全局异常处理
│   ├── mapper/                   # MyBatis-Plus Mapper
│   ├── service/                  # 服务层接口与实现
│   └── utils/                    # 工具类
│       ├── DFAUtil.java          # DFA 敏感词过滤算法
│       ├── TextAnalysis.java     # 文本分析工具
│       ├── TokenUtils.java       # JWT Token 工具
│       └── TreeGenerator.java    # 树形结构生成器
├── src/main/resources/
│   └── application.yaml          # 主配置文件
├── vue/                          # 前端项目
│   ├── src/
│   │   ├── views/
│   │   │   ├── front/            # 前台页面（9 个页面）
│   │   │   ├── back/             # 后台管理页面（10 个页面）
│   │   │   ├── Login.vue         # 登录页
│   │   │   ├── Register.vue      # 注册页
│   │   │   ├── Back.vue          # 后台布局
│   │   │   └── Front.vue         # 前台布局
│   │   ├── components/           # 通用组件
│   │   ├── router/               # 路由配置
│   │   ├── utils/                # 工具函数（Axios 封装等）
│   │   └── style/                # 全局样式
│   ├── config/                   # 前端配置
│   └── vite.config.js            # Vite 构建配置
├── files/                        # 用户上传文件目录
├── pom.xml                       # Maven 依赖配置
└── README.md
```

## API 接口

| 前缀 | 说明 |
|------|------|
| `/web/` | 登录、注册、文件上传下载 |
| `/user/` | 用户管理接口 |
| `/admin/` | 管理员管理接口 |
| `/blog/` | 游记内容接口 |
| `/type/` | 分类管理接口 |
| `/comment/` | 评论管理接口 |
| `/collect/` | 收藏管理接口 |
| `/follow/` | 关注管理接口 |
| `/message/` | 消息管理接口 |
| `/word/` | 敏感词管理接口 |
| `/ai/` | AI 问答接口 |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20+
- Maven 3.6+
- MySQL 8.0+

### 1. 数据库

```sql
CREATE DATABASE springboot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 根据实体类自动建表（MyBatis-Plus 自动生成），或导入 SQL 脚本。

### 2. 后端启动

```bash
# 1. 修改数据库配置
#    编辑 src/main/resources/application.yaml
#    配置数据库地址、用户名、密码

# 2. 启动后端
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

# 运行
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

## AI 模块配置

项目集成了 DeepSeek 大模型和文本嵌入服务，配置方式：

```yaml
ai:
  deepseek:
    api-key: your_api_key_here
    api-url: https://api.deepseek.com
  embedding:
    model: text-embedding-3-small
```

## 许可证

MIT