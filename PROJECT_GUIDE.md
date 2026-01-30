# 教育资源平台项目 - 完整创建指南

## 项目概述
本项目是一个教育资源平台，包含前端（Vue 3）和后端（Servlet + JSP + MySQL + Tomcat）。

## 项目结构
```
Graduation-Design-EduResourcePlatform/
├── frontend-project/
│   └── edu-resource-frontend/      # Vue 3 前端项目
└── backend-project/
    └── edu-resource-backend/       # Java Web 后端项目
```

---

## 第一部分：前端项目（Vue 3）

### 步骤1：创建Vue 3项目

#### 完整创建命令
```bash
# 进入项目根目录
cd C:\APP\Graduation-Design-EduResourcePlatform

# 创建前端项目文件夹
mkdir frontend-project
cd frontend-project

# 使用Vue CLI创建Vue 3项目
vue create edu-resource-frontend --preset default --packageManager npm --force

# 进入项目目录
cd edu-resource-frontend

# 安装Vue Router、Vuex、Less和Axios
npm install vue-router@4 vuex@4 less less-loader axios --save
```

#### 技术栈说明
- **Vue 3**: 前端框架
- **Vue Router 4**: 路由管理
- **Vuex 4**: 状态管理
- **Less**: CSS预处理器
- **Axios**: HTTP客户端
- **Babel**: JavaScript编译器

### 步骤2：启动前端项目

#### 启动命令
```bash
# 进入前端项目目录
cd C:\APP\Graduation-Design-EduResourcePlatform\frontend-project\edu-resource-frontend

# 启动开发服务器
npm run serve
```

#### 访问地址
- **前端应用**: http://localhost:8080/

### 步骤3：解决"Running completion hooks..."卡住问题

#### 问题原因
Vue CLI在执行completion hooks时可能会因为网络问题或依赖下载问题而卡住。

#### 解决方案
```bash
# 1. 按 Ctrl + C 停止当前进程

# 2. 删除node_modules文件夹和package-lock.json
rm -rf node_modules
rm package-lock.json

# 3. 清理npm缓存
npm cache clean --force

# 4. 重新安装依赖
npm install

# 5. 重新启动开发服务器
npm run serve
```

#### 其他解决方案
```bash
# 如果上述方法无效，尝试使用国内镜像源
npm config set registry https://registry.npmmirror.com

# 然后重新安装
npm install
npm run serve
```

### 步骤4：验证前端项目

#### 验证方式
1. **检查开发服务器是否启动成功**
   - 在终端查看输出，应该看到：
   ```
   App running at:
   - Local:   http://localhost:8080/
   - Network: http://192.168.x.x:8080/
   ```

2. **在浏览器中访问**
   - 打开浏览器访问: http://localhost:8080/
   - 应该看到Vue的欢迎页面

3. **检查依赖是否安装成功**
   - 查看 [package.json](file:///c:/APP/Graduation-Design-EduResourcePlatform/frontend-project/edu-resource-frontend/package.json) 文件
   - 确认包含以下依赖：
     - vue
     - vue-router
     - vuex
     - less
     - less-loader
     - axios

---

## 第二部分：后端项目（Java Web）

### 步骤1：创建Java Web项目结构

#### 完整创建命令（PowerShell）
```powershell
# 进入项目根目录
cd C:\APP\Graduation-Design-EduResourcePlatform

# 创建后端项目文件夹
mkdir backend-project
cd backend-project
mkdir edu-resource-backend
cd edu-resource-backend

# 创建Maven标准目录结构
New-Item -ItemType Directory -Force -Path "src\main\java"
New-Item -ItemType Directory -Force -Path "src\main\resources"
New-Item -ItemType Directory -Force -Path "src\main\webapp"
New-Item -ItemType Directory -Force -Path "src\main\webapp\WEB-INF"
New-Item -ItemType Directory -Force -Path "src\test\java"

# 创建包结构
New-Item -ItemType Directory -Force -Path "src\main\java\com\edu\servlet"
New-Item -ItemType Directory -Force -Path "src\main\java\com\edu\filter"
```

#### 技术栈说明
- **Servlet**: Java Web核心组件
- **JSP**: JavaServer Pages
- **MySQL**: 数据库
- **Tomcat**: Web服务器
- **Maven**: 项目构建工具
- **Gson**: JSON处理库
- **Druid**: 数据库连接池

### 步骤2：配置Maven构建文件

#### [pom.xml](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/pom.xml) 配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.edu</groupId>
    <artifactId>edu-resource-backend</artifactId>
    <version>1.0.0</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Servlet API -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSP API -->
        <dependency>
            <groupId>javax.servlet.jsp</groupId>
            <artifactId>javax.servlet.jsp-api</artifactId>
            <version>2.3.3</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSTL -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>

        <!-- MySQL Connector -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>

        <!-- Druid Connection Pool -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.2.18</version>
        </dependency>

        <!-- Gson for JSON -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>

        <!-- File Upload -->
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
            <version>1.5</version>
        </dependency>

        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.11.0</version>
        </dependency>
    </dependencies>

    <build>
        <finalName>edu-resource-backend</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.3.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 步骤3：配置数据库

#### 数据库初始化脚本
```bash
# 登录MySQL
mysql -u root -p
# 输入密码: root

# 执行初始化脚本
source C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend\src\main\resources\init.sql
```

#### 或直接执行SQL文件
```bash
mysql -u root -p < C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend\src\main\resources\init.sql
```

#### 验证数据库
```bash
mysql -u root -p
# 输入密码: root

USE edu_resource;
SHOW TABLES;
```

应该看到以下表：
- users
- resources
- categories

### 步骤4：构建项目

#### 构建命令
```bash
# 进入后端项目目录
cd C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend

# 清理并构建项目
mvn clean package
```

#### 构建结果
构建成功后，会在 `target` 目录下生成 WAR 文件：
```
target/edu-resource-backend.war
```

### 步骤5：部署到Tomcat

#### 部署方式1：手动部署
```bash
# 复制WAR文件到Tomcat webapps目录
copy target\edu-resource-backend.war C:\apache-tomcat-9.0.x\webapps\

# 启动Tomcat
cd C:\apache-tomcat-9.0.x\bin
startup.bat
```

#### 部署方式2：使用Maven Tomcat Plugin（需要额外配置）
在 [pom.xml](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/pom.xml) 中添加：
```xml
<plugin>
    <groupId>org.apache.tomcat.maven</groupId>
    <artifactId>tomcat7-maven-plugin</artifactId>
    <version>2.2</version>
    <configuration>
        <url>http://localhost:8080/manager/text</url>
        <server>tomcat</server>
        <path>/edu-resource-backend</path>
    </configuration>
</plugin>
```

然后运行：
```bash
mvn tomcat7:deploy
```

### 步骤6：启动后端项目

#### 启动Tomcat
```bash
cd C:\apache-tomcat-9.0.x\bin
startup.bat
```

#### 访问地址
- **首页**: http://localhost:8080/edu-resource-backend/
- **Hello Servlet**: http://localhost:8080/edu-resource-backend/hello
- **Resources API**: http://localhost:8080/edu-resource-backend/api/resources

### 步骤7：验证后端项目

#### 验证方式
1. **检查Tomcat是否启动成功**
   - 在终端查看输出，应该看到：
   ```
   Server startup in [xxx] ms
   ```

2. **在浏览器中访问首页**
   - 访问: http://localhost:8080/edu-resource-backend/
   - 应该看到欢迎页面和项目信息

3. **测试Hello Servlet**
   - 访问: http://localhost:8080/edu-resource-backend/hello
   - 应该看到 "Welcome to Edu Resource Platform Backend!" 消息

4. **测试API接口**
   - 访问: http://localhost:8080/edu-resource-backend/api/resources
   - 应该看到JSON格式的资源数据

---

## 第三部分：项目文件说明

### 前端项目关键文件

#### [package.json](file:///c:/APP/Graduation-Design-EduResourcePlatform/frontend-project/edu-resource-frontend/package.json)
定义项目依赖和脚本命令。

#### [src/main.js](file:///c:/APP/Graduation-Design-EduResourcePlatform/frontend-project/edu-resource-frontend/src/main.js)
应用的入口文件。

#### [src/App.vue](file:///c:/APP/Graduation-Design-EduResourcePlatform/frontend-project/edu-resource-frontend/src/App.vue)
根组件。

### 后端项目关键文件

#### [pom.xml](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/pom.xml)
Maven项目配置文件。

#### [src/main/webapp/WEB-INF/web.xml](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/webapp/WEB-INF/web.xml)
Web应用配置文件。

#### [src/main/java/com/edu/servlet/HelloServlet.java](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/java/com/edu/servlet/HelloServlet.java)
示例Servlet。

#### [src/main/java/com/edu/servlet/ResourceServlet.java](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/java/com/edu/servlet/ResourceServlet.java)
资源API Servlet。

#### [src/main/java/com/edu/filter/CharacterEncodingFilter.java](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/java/com/edu/filter/CharacterEncodingFilter.java)
字符编码过滤器。

#### [src/main/webapp/index.jsp](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/webapp/index.jsp)
首页JSP文件。

#### [src/main/resources/database.properties](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/resources/database.properties)
数据库配置文件。

#### [src/main/resources/init.sql](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/resources/init.sql)
数据库初始化脚本。

---

## 第四部分：常见问题解决

### 前端问题

#### 问题1：端口8080被占用
**解决方案**：修改 [vue.config.js](file:///c:/APP/Graduation-Design-EduResourcePlatform/frontend-project/edu-resource-frontend/vue.config.js)
```javascript
module.exports = {
  devServer: {
    port: 8081
  }
}
```

#### 问题2：依赖安装失败
**解决方案**：
```bash
# 清理缓存
npm cache clean --force

# 使用国内镜像
npm config set registry https://registry.npmmirror.com

# 重新安装
npm install
```

#### 问题3：Vue CLI命令未找到
**解决方案**：
```bash
# 全局安装Vue CLI
npm install -g @vue/cli

# 验证安装
vue --version
```

### 后端问题

#### 问题1：Maven命令未找到
**解决方案**：
1. 下载并安装Maven：https://maven.apache.org/download.cgi
2. 配置环境变量：
   - 添加 `MAVEN_HOME` 环境变量指向Maven安装目录
   - 将 `%MAVEN_HOME%\bin` 添加到 `PATH` 环境变量
3. 验证安装：
```bash
mvn -version
```

#### 问题2：数据库连接失败
**解决方案**：
1. 检查MySQL服务是否启动
2. 验证数据库配置在 [web.xml](file:///c:/APP/Graduation-Design-EduResourcePlatform/backend-project/edu-resource-backend/src/main/webapp/WEB-INF/web.xml) 中是否正确
3. 确保数据库 `edu_resource` 已创建

#### 问题3：Tomcat启动失败
**解决方案**：
1. 检查JAVA_HOME环境变量是否正确配置
2. 检查端口8080是否被占用
3. 查看Tomcat日志文件：`logs/catalina.out`

#### 问题4：编译错误
**解决方案**：
```bash
# 清理并重新构建
mvn clean install

# 如果依赖下载失败，尝试使用阿里云镜像
# 在Maven的settings.xml中配置镜像源
```

---

## 第五部分：开发建议

### 前端开发建议

1. **组件化开发**
   - 将UI拆分为可复用的组件
   - 使用Vue 3 Composition API

2. **路由管理**
   - 使用Vue Router进行页面导航
   - 配置路由守卫进行权限控制

3. **状态管理**
   - 使用Vuex管理全局状态
   - 将API调用封装在actions中

4. **样式管理**
   - 使用Less编写样式
   - 定义全局样式变量

### 后端开发建议

1. **分层架构**
   - Controller层：处理HTTP请求
   - Service层：业务逻辑
   - DAO层：数据访问

2. **数据库操作**
   - 使用PreparedStatement防止SQL注入
   - 使用连接池提高性能

3. **异常处理**
   - 统一异常处理机制
   - 友好的错误提示

4. **安全性**
   - 实现用户认证和授权
   - 防止XSS和CSRF攻击

---

## 第六部分：下一步工作

### 前端待完成功能
- [ ] 配置Vue Router
- [ ] 配置Vuex Store
- [ ] 创建页面组件（首页、资源列表、详情页等）
- [ ] 实现API调用封装
- [ ] 添加用户认证功能
- [ ] 实现资源上传功能

### 后端待完成功能
- [ ] 实现完整的CRUD操作
- [ ] 添加用户登录注册功能
- [ ] 实现文件上传下载
- [ ] 添加分页查询
- [ ] 实现搜索功能
- [ ] 添加权限控制

---

## 总结

本项目已成功创建，包含：
- ✅ Vue 3前端项目（包含Router、Vuex、Less）
- ✅ Java Web后端项目（Servlet + JSP + MySQL + Tomcat）
- ✅ 完整的项目结构和配置文件
- ✅ 示例代码和API接口
- ✅ 数据库初始化脚本

所有文件名、类名、方法名均为英文，符合项目要求。
