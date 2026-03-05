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
├── constant/              # 常量 (UserConstant, AppConstant)
├── controller/            # API 控制器 (UserController, AppController, StaticResourceController)
├── core/                  # 核心业务模块
│   ├── parser/            # 代码解析器（策略模式）
│   ├── saver/             # 文件保存器（模板方法模式）
│   ├── CodeGenTypeEnum.java
│   └── AiCodeGeneratorFacade.java
├── exception/             # 异常处理
├── mapper/                # MyBatis Mapper
├── model/
│   ├── dto/               # 数据传输对象 (AppAddRequest, AppDeployRequest, etc.)
│   ├── entity/            # 实体类 (User, App)
│   ├── enums/             # 枚举类
│   └── vo/                # 视图对象 (UserVO, AppVO)
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

### 应用模块
| 方法 | 路径 | 说明 | 权限 |
|-----|------|-----|-----|
| POST | `/api/app/add` | 创建应用 | 需登录 |
| POST | `/api/app/update` | 更新应用 | 所有者 |
| POST | `/api/app/delete` | 删除应用 | 所有者/管理员 |
| GET | `/api/app/get/vo` | 获取应用详情 | 无 |
| POST | `/api/app/my/list/page/vo` | 分页查询我的应用 | 需登录 |
| POST | `/api/app/good/list/page/vo` | 分页查询精选应用 | 无 |
| GET | `/api/app/chat/gen/code` | AI 代码生成（SSE流式） | 所有者 |
| POST | `/api/app/deploy` | 部署应用 | 所有者 |

### 管理员接口
| 方法 | 路径 | 说明 | 权限 |
|-----|------|-----|-----|
| POST | `/api/app/admin/delete` | 删除任意应用 | 管理员 |
| POST | `/api/app/admin/update` | 更新应用（限制字段） | 管理员 |
| POST | `/api/app/admin/list/page/vo` | 分页查询所有应用 | 管理员 |
| GET | `/api/app/admin/get/vo` | 获取应用详情 | 管理员 |

### 静态资源
| 方法 | 路径 | 说明 | 权限 |
|-----|------|-----|-----|
| GET | `/api/static/{deployKey}/**` | 访问已部署应用 | 无 |

## AI 代码生成

### 功能特性
- **同步生成**: 支持单文件 HTML 和多文件代码生成
- **流式输出**: SSE 流式返回，实时展示生成进度
- **自动保存**: 生成的代码自动保存到 `tmp/code_output/` 目录
- **代码部署**: 一键部署，生成可访问的 URL

### SSE 流式接口
```
GET /api/app/chat/gen/code?appId=1&message=生成一个登录页面
Accept: text/event-stream
```

**响应格式**:
```
data: {"d":"<html>\n<head>"}
data: {"d":"<title>Login</title>"}
event: done
data: 
```

### 前端对接示例
```javascript
const eventSource = new EventSource('/api/app/chat/gen/code?appId=1&message=xxx');

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log(data.d); // 获取实际内容
};

eventSource.addEventListener('done', (event) => {
    console.log('生成完成');
    eventSource.close();
});
```

## 应用部署

### 部署流程
```
1. 生成代码 → tmp/code_output/HTML_1/
2. 调用部署接口 → 生成 deployKey (如 "aB3xYz")
3. 复制文件 → tmp/code_deploy/aB3xYz/
4. 更新数据库 → deployKey, deployedTime
5. 访问 URL → http://localhost/aB3xYz/
```

### 目录结构
```
tmp/
├── code_output/           # AI 生成的代码（预览）
│   ├── HTML_1/            # 格式: codeGenType_appId
│   └── MULTI_FILE_2/
└── code_deploy/           # 部署的代码（正式访问）
    ├── aB3xYz/           # deployKey 作为目录名
    └── xY7zAb/
```

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