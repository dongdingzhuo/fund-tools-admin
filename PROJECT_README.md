# Fund Tools Admin 项目说明

## 项目简介
这是一个基于 Spring Boot + MyBatis-Plus + MySQL + Hutool 的基金工具管理后台项目。

## 技术栈
- **Spring Boot 2.7.18**: 基础框架
- **MyBatis-Plus 3.5.3.1**: ORM框架，简化数据库操作
- **MySQL**: 数据库
- **Hutool 5.8.22**: Java工具类库
- **Lombok**: 简化代码编写

## 项目结构
```
fund-tools-admin
├── src/main/java/com/fund/tools/api
│   ├── common              # 通用类
│   │   └── Result.java     # 统一返回结果
│   ├── config              # 配置类
│   │   └── MybatisPlusConfig.java  # MyBatis-Plus配置
│   ├── controller          # 控制器层
│   │   ├── HelloController.java
│   │   └── AuthController.java     # 认证控制器（登录）
│   ├── dto                 # 数据传输对象
│   │   ├── LoginRequest.java       # 登录请求
│   │   └── LoginResponse.java      # 登录响应
│   ├── entity              # 实体类
│   │   └── User.java       # 用户实体
│   ├── mapper              # Mapper接口
│   │   └── UserMapper.java
│   ├── service             # 服务层
│   │   ├── UserService.java
│   │   └── impl
│   │       └── UserServiceImpl.java
│   └── FundToolsAdminApplication.java  # 启动类
├── src/main/resources
│   ├── application.yml     # 配置文件
│   └── sql
│       └── init.sql        # 数据库初始化脚本
└── pom.xml                 # Maven配置文件
```

## 快速开始

### 1. 环境要求
- JDK 11+
- Maven 3.6+
- MySQL 5.7+ 或 8.0+

### 2. 数据库配置
1. 创建数据库并执行初始化脚本：
```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

2. 修改 `application.yml` 中的数据库配置（或使用环境变量）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fund?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

或者设置环境变量：
- `DB_HOST`: 数据库主机地址
- `DB_PORT`: 数据库端口
- `DB_NAME`: 数据库名称
- `DB_USERNAME`: 数据库用户名
- `DB_PASSWORD`: 数据库密码

### 3. 运行项目
```bash
mvn clean install
mvn spring-boot:run
```

或者直接运行主类：`FundToolsAdminApplication`

### 4. 访问接口
项目启动后，访问 http://localhost:8080

## API 接口说明

### 认证接口

#### 1. 用户登录
```
POST /auth/login
Content-Type: application/json

{
  "userName": "admin"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "userName": "admin",
    "nickName": "管理员"
  }
}
```

**说明：**
- 只需要用户名存在即验证通过
- 不需要密码验证
- 返回用户基本信息

## 主要特性

### MyBatis-Plus 功能
- ✅ 自动CRUD操作
- ✅ 条件构造器
- ✅ 主键自动生成

### Hutool 工具
- ✅ 字符串工具（StrUtil）
- ✅ 日期时间工具
- ✅ JSON工具
- ✅ 其他常用工具类

## 测试用户

数据库初始化后，有以下测试用户：

| 用户名 | 昵称 | 说明 |
|--------|------|------|
| admin | 管理员 | 管理员账号 |
| test | 测试用户 | 测试账号 |
| user1 | 用户一 | 普通用户 |

## 注意事项

1. **数据库密码**: 请根据实际情况修改 `application.yml` 中的数据库密码或设置环境变量
2. **端口配置**: 默认端口为8080，可通过 `server.port` 修改
3. **Mapper扫描**: 已在启动类添加 `@MapperScan` 注解，自动扫描Mapper接口
4. **简单验证**: 登录接口只需要用户名存在即可通过验证，无需密码

## 开发建议

1. 新增业务模块时，按照以下顺序创建文件：
   - DTO（数据传输对象）
   - Entity（实体类）
   - Mapper（Mapper接口）
   - Service（服务接口）
   - ServiceImpl（服务实现类）
   - Controller（控制器）

2. 使用 MyBatis-Plus 的代码生成器可以快速生成基础代码

3. 建议使用 Postman 或 Swagger 进行接口测试

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 检查数据库名、用户名、密码是否正确
- 检查防火墙设置

### 2. 依赖下载失败
- 检查Maven配置
- 尝试更换Maven镜像源

### 3. 端口被占用
- 修改 `application.properties` 中的 `server.port`
