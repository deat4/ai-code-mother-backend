package com.zkf.aicodemother.controller;

import com.zkf.aicodemother.constant.AppConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * 静态资源控制器
 */
@RestController
public class StaticResourceController {

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/static/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());

            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }

            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 构建文件路径
            String filePath = AppConstant.CODE_DEPLOY_ROOT_DIR + "/" + deployKey + resourcePath;
            File file = new File(filePath);

            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        if (filePath.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filePath.endsWith(".woff") || filePath.endsWith(".woff2")) return "font/woff2";
        if (filePath.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    /**
     * 预览生成的代码（生成目录）
     * 访问格式：http://localhost:8123/api/preview/{codeGenType_appId}[/{fileName}]
     *
     * 对于 Vue 项目（vue_project_），优先使用构建后的 dist 目录
     */
    @GetMapping("/preview/{codeGenType_appId}/**")
    public ResponseEntity<Resource> previewGeneratedCode(
            @PathVariable("codeGenType_appId") String codeGenTypeAppId,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/preview/" + codeGenTypeAppId).length());

            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }

            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }

            // 对于 Vue 项目，优先使用 dist 目录
            String baseDir;
            if (codeGenTypeAppId.startsWith("vue_project_")) {
                // 检查 dist 目录是否存在
                String distDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/" + codeGenTypeAppId + "/dist";
                File distFolder = new File(distDir);
                if (distFolder.exists() && distFolder.isDirectory()) {
                    baseDir = distDir;
                } else {
                    // dist 不存在，返回提示页面
                    if (resourcePath.equals("/index.html")) {
                        return ResponseEntity.ok()
                                .header("Content-Type", "text/html; charset=UTF-8")
                                .body(new FileSystemResource(createVueBuildingPage()));
                    }
                    // 其他资源请求返回 404
                    return ResponseEntity.notFound().build();
                }
            } else {
                // 其他类型直接使用源目录
                baseDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/" + codeGenTypeAppId;
            }

            // 构建文件路径
            String filePath = baseDir + resourcePath;
            File file = new File(filePath);

            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 创建 Vue 项目构建等待页面
     */
    private File createVueBuildingPage() {
        String htmlContent = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vue 项目构建中</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh; background: #f5f5f7;
        }
        .container {
            text-align: center; padding: 40px;
            background: #fff; border-radius: 16px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            max-width: 400px;
        }
        .icon { font-size: 48px; margin-bottom: 20px; }
        h1 { color: #1d1d1f; margin-bottom: 12px; }
        p { color: #86868b; line-height: 1.5; }
        .spinner {
            width: 40px; height: 40px;
            border: 3px solid #e0e0e0;
            border-top-color: #0071e3;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .tip { color: #0071e3; font-size: 14px; margin-top: 16px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">⚙️</div>
        <h1>Vue 项目构建中</h1>
        <p>项目正在执行 npm install 和 npm run build，请稍候...</p>
        <div class="spinner"></div>
        <p class="tip">构建完成后刷新页面即可预览</p>
    </div>
    <script>
        // 每3秒刷新检查构建是否完成
        setInterval(() => { location.reload(); }, 3000);
    </script>
</body>
</html>
""".stripIndent();

        // 写入临时文件
        File tempFile = new File(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_building_tip.html");
        try {
            if (!tempFile.exists()) {
                java.nio.file.Files.writeString(tempFile.toPath(), htmlContent);
            }
        } catch (Exception e) {
            // 如果写入失败，创建内存中的临时文件
            try {
                tempFile = File.createTempFile("vue_building", ".html");
                java.nio.file.Files.writeString(tempFile.toPath(), htmlContent);
            } catch (Exception ex) {
                // 忽略，使用默认处理
            }
        }
        return tempFile;
    }
}