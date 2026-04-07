package com.zkf.aicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.zkf.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = resolvePath(relativeFilePath, appId);
            
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            // 写入文件内容
            Files.write(path, content.getBytes("UTF-8"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private Path resolvePath(String relativeFilePath, Long appId) {
        Path path = Paths.get(relativeFilePath);
        if (!path.isAbsolute()) {
            String projectDirName = "vue_project_" + appId;
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
            path = projectRoot.resolve(relativeFilePath);
        }
        return path;
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.get("relativeFilePath", String.class);
        String content = arguments.get("content", String.class);
        String suffix = FileUtil.getSuffix(relativeFilePath);
        // 推断语言类型
        String language = inferLanguage(suffix);

        // 返回结构化 JSON，便于前端解析
        JSONObject result = new JSONObject();
        result.set("type", "tool_call");
        result.set("toolName", getToolName());
        result.set("fileName", relativeFilePath);
        result.set("language", language);
        result.set("content", content);
        return result.toString();
    }

    /**
     * 根据文件扩展名推断语言类型
     */
    private String inferLanguage(String suffix) {
        if (suffix == null) return "text";
        return switch (suffix.toLowerCase()) {
            case "vue" -> "vue";
            case "html", "htm" -> "html";
            case "css", "scss", "sass", "less" -> "css";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "jsx" -> "jsx";
            case "tsx" -> "tsx";
            case "json" -> "json";
            case "md" -> "markdown";
            case "java" -> "java";
            case "py" -> "python";
            case "xml" -> "xml";
            case "yaml", "yml" -> "yaml";
            default -> suffix;
        };
    }
}

