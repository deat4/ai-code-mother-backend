package com.zkf.aicodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.zkf.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录读取工具
 * 递归获取目录下所有文件结构
 */
@Slf4j
@Component
public class DirectoryListTool extends BaseTool {

    @Tool("递归获取目录下的所有文件结构")
    public String listDirectory(
            @P("目录的相对路径，空字符串表示项目根目录")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path projectRoot = getProjectRoot(appId);
            Path targetDir = StrUtil.isBlank(relativeDirPath) 
                    ? projectRoot 
                    : projectRoot.resolve(relativeDirPath);
            
            if (!Files.exists(targetDir)) {
                return "目录不存在: " + relativeDirPath;
            }
            if (!Files.isDirectory(targetDir)) {
                return "指定路径不是目录: " + relativeDirPath;
            }

            List<String> fileTree = new ArrayList<>();
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String relativePath = projectRoot.relativize(dir).toString().replace('\\', '/');
                    fileTree.add(relativePath + "/");
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = projectRoot.relativize(file).toString().replace('\\', '/');
                    fileTree.add(relativePath);
                    return FileVisitResult.CONTINUE;
                }
            });

            String result = String.join("\n", fileTree);
            log.info("成功获取目录结构: {}, 文件/目录数量: {}", targetDir.toAbsolutePath(), fileTree.size());
            return result;
        } catch (IOException e) {
            String errorMessage = "目录读取失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    private Path getProjectRoot(Long appId) {
        String projectDirName = "vue_project_" + appId;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
    }

    @Override
    public String getToolName() {
        return "listDirectory";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.get("relativeDirPath", String.class);
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }
}