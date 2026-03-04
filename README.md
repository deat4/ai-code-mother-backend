# AI Code Mother Backend

智能代码生成平台后端服务 | Spring Boot 3.5.11 + Java 21 + MyBatis-Flex + LangChain4j

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
├── ai/                    # AI 代码生成模块
│   ├── model/             # AI 模型类 (HtmlCodeResult, MultiFileCodeResult)
│   └── AiCodeGeneratorService.java
├── annotation/            # 自定义注解 (AuthCheck)
├── aop/                   # AOP切面 (AuthInterceptor)
├── common/                # 通用类 (BaseResponse, ResultUtils, PageRequest)
├── config/                # 配置类 (CorsConfig, JsonConfig, AiCodeGeneratorServiceFactory)
├── constant/              # 常量 (UserConstant)
├── controller/            # API 控制器
├── core/                  # 核心业务模块
│   ├── parser/            # 代码解析器（策略模式）
│   ├── saver/             # 文件保存器（模板方法模式）
│   ├── CodeGenTypeEnum.java
│   └── AiCodeGeneratorFacade.java
├── exception/             # 异常处理
├── mapper/                # MyBatis Mapper
├── model/
│   ├── dto/               # 数据传输对象
│   ├── entity/            # 实体类
│   ├── enums/             # 枚举类
│   └── vo/                # 视图对象
└── service/               # 业务逻辑
```

## 核心功能

### 用户模块
| 方法 | 路径 | 说明 | 权限 |
|-----|------|-----|-----|
| POST | `/api/user/register` | 用户注册 | 无 |
| POST | `/api/user/login` | 用户登录 | 无 |
| POST | `/api/user/logout` | 用户注销 | 需登录 |
| GET | `/api/user/get/login` | 获取当前用户 | 需登录 |
| POST | `/api/user/add` | 创建用户 | 管理员 |
| POST | `/api/user/delete` | 删除用户 | 管理员 |
| POST | `/api/user/update` | 更新用户 | 管理员 |
| POST | `/api/user/list/page/vo` | 分页查询用户 | 管理员 |

### AI 代码生成
- **同步生成**: 支持单文件 HTML 和多文件代码生成
- **流式输出**: SSE 流式返回，实时展示生成进度
- **自动保存**: 生成的代码自动保存到 `tmp/code_output/` 目录

## 设计模式

| 模式 | 应用场景 |
|------|----------|
| 门面模式 | `AiCodeGeneratorFacade` 统一入口 |
| 策略模式 | `CodeParser` 不同类型解析策略 |
| 模板方法模式 | `CodeFileSaverTemplate` 统一保存流程 |

## 配置说明

```yaml
# application-location.yml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
      max-tokens: 8192
    streaming-chat-model:
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
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

Spring Boot 3.5.11 · Java 21 · MySQL · MyBatis-Flex · LangChain4j · Reactor · Knife4j · Hutool · Lombok

## License

MIT