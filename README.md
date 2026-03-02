# AI Code Mother Backend

基于 Spring Boot 的智能代码生成平台后端服务。

## 项目简介

本项目是一个 RESTful API 后端服务，提供智能代码生成能力，采用 Spring Boot 3.x + Java 21 技术栈。

## 技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| Spring Boot | 3.5.11 | 基础框架 |
| Java | 21 | JDK 版本 |
| MySQL | - | 数据库 |
| Knife4j | 4.4.0 | API 文档 |
| Hutool | 5.8.38 | 工具库 |
| Lombok | - | 简化代码 |

## 项目结构

```
src/main/java/com/zkf/aicodemother/
├── common/              # 通用模块
│   ├── BaseResponse.java    # 统一响应封装
│   ├── DeleteRequest.java   # 删除请求
│   ├── PageRequest.java     # 分页请求
│   └── ResultUtils.java     # 响应工具类
├── config/              # 配置模块
│   └── CorsConfig.java      # 跨域配置
├── controller/          # 控制器层
├── exception/           # 异常处理
│   ├── ErrorCode.java           # 错误码枚举
│   ├── BusinessException.java   # 业务异常
│   ├── ThrowUtils.java          # 异常工具类
│   └── GlobalExceptionHandler.java
├── service/             # 服务层
├── repository/          # 数据访问层
├── entity/              # 实体类
├── dto/                 # 数据传输对象
└── AiCodeMotherApplication.java  # 启动类
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 本地运行

```bash
# 克隆项目
git clone https://github.com/deat4/ai-code-mother-backend.git
cd ai-code-mother-backend

# 编译项目
mvn clean install

# 运行项目
mvn spring-boot:run
```

### 测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassName

# 运行单个测试方法
mvn test -Dtest=ClassName#methodName
```

## API 文档

启动项目后访问：
- Knife4j UI: http://localhost:8123/api/doc.html
- OpenAPI JSON: http://localhost:8123/api/v3/api-docs

## 错误码说明

| 错误码 | 说明 |
|-------|-----|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40300 | 禁止访问 |
| 40400 | 请求数据不存在 |
| 50000 | 系统内部异常 |
| 50001 | 操作失败 |

## 配置说明

应用配置文件位于 `src/main/resources/application.yml`：

```yaml
server:
  port: 8123
  servlet:
    context-path: /api
```

## 开发指南

详细的开发指南请参考 [AGENTS.md](./AGENTS.md)。

## License

MIT License