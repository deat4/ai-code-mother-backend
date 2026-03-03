# AI Code Mother Backend

智能代码生成平台后端服务 | Spring Boot 3.5.11 + Java 21 + MyBatis-Flex

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
├── annotation/      # 自定义注解 (AuthCheck)
├── aop/             # AOP切面 (AuthInterceptor)
├── common/          # 通用类 (BaseResponse, ResultUtils, PageRequest)
├── config/          # 配置类 (CorsConfig, JsonConfig)
├── constant/        # 常量 (UserConstant)
├── controller/      # API控制器
├── exception/       # 异常处理
├── mapper/          # MyBatis Mapper
├── model/
│   ├── dto/         # 数据传输对象
│   ├── entity/      # 实体类
│   ├── enums/       # 枚举类
│   └── vo/          # 视图对象
└── service/         # 业务逻辑
```

## API 接口

### 用户模块
| 方法 | 路径 | 说明 | 权限 |
|-----|------|-----|-----|
| POST | `/api/user/register` | 用户注册 | 无 |
| POST | `/api/user/login` | 用户登录 | 无 |
| POST | `/api/user/logout` | 用户注销 | 需登录 |
| GET | `/api/user/get/login` | 获取当前用户 | 需登录 |
| GET | `/api/user/get/vo/{id}` | 获取用户(脱敏) | 无 |
| POST | `/api/user/add` | 创建用户 | 管理员 |
| POST | `/api/user/delete` | 删除用户 | 管理员 |
| POST | `/api/user/update` | 更新用户 | 管理员 |
| POST | `/api/user/list/page/vo` | 分页查询用户 | 管理员 |

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

Spring Boot 3.5.11 · Java 21 · MySQL · MyBatis-Flex · Knife4j · Hutool · Lombok

## License

MIT

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