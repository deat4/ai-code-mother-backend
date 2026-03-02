# AI Code Mother Backend

智能代码生成平台后端服务 | Spring Boot 3.5.11 + Java 21

## 快速开始

```bash
# 编译
mvn clean install

# 运行
mvn spring-boot:run

# 测试
mvn test -Dtest=ClassName#methodName
```

## 项目结构

```
com.zkf.aicodemother
├── common/          # 通用响应、分页、工具类
├── config/          # 跨域等配置
├── controller/      # API 控制器
├── service/         # 业务逻辑
├── repository/      # 数据访问
├── entity/          # 实体类
├── dto/             # 数据传输对象
└── exception/       # 异常处理
```

## API 文档

启动后访问：http://localhost:8123/api/doc.html

## 错误码

| 码 | 说明 | 码 | 说明 |
|---|-----|---|-----|
| 0 | 成功 | 40101 | 无权限 |
| 40000 | 参数错误 | 40300 | 禁止访问 |
| 40100 | 未登录 | 40400 | 数据不存在 |
| 50000 | 系统异常 | 50001 | 操作失败 |

## 技术栈

Spring Boot 3.5.11 · Java 21 · MySQL · Knife4j · Hutool · Lombok

## License

MIT