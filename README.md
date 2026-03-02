# AI Code Mother Backend

基于 Spring Boot 的 AI 代码生成平台后端服务。

## 技术栈

- Java 21
- Spring Boot 3.5.11
- MySQL
- Knife4j (OpenAPI 3.0)

## 快速开始

```bash
# 安装依赖
mvn clean install

# 启动服务
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8123/api/doc.html
- 健康检查：http://localhost:8123/api/health

## 项目结构

```
src/main/java/com/zkf/aicodemother/
├── common/          # 通用类（响应、分页等）
├── config/          # 配置类（跨域等）
├── controller/      # 控制器层
├── exception/       # 异常处理
├── service/         # 业务逻辑层
├── repository/      # 数据访问层
├── entity/          # 实体类
└── dto/             # 数据传输对象
```

## 测试

```bash
mvn test
```