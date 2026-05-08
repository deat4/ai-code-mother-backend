package com.zkf.aicodemother.core.repair;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationIssue;
import com.zkf.aicodemother.core.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 修复提示词构建器
 * 根据 ValidationResult 和项目上下文生成最小必要修复的 prompt
 */
@Slf4j
@Component
public class RepairPromptBuilder {

    private static final int MAX_FILE_CONTENT_LENGTH = 5000;

    /**
     * 构建修复提示词
     *
     * @param context 修复上下文
     * @return 修复提示词
     */
    public String buildRepairPrompt(RepairContext context) {
        StringBuilder promptBuilder = new StringBuilder();

        ValidationResult validationResult = context.getValidationResult();
        String codeGenType = context.getCodeGenType();
        String scene = context.getScene();
        Long appId = context.getAppId();

        // 1. 修复任务说明
        promptBuilder.append("【修复任务】\n");
        promptBuilder.append("- 项目类型: ").append(codeGenType).append("\n");
        promptBuilder.append("- 场景: ").append(scene).append("\n");
        promptBuilder.append("- 这是第 ").append(context.getRepairRound()).append(" 次自动修复\n\n");

        // 2. 失败原因
        promptBuilder.append("【失败原因】\n");
        promptBuilder.append("- 校验摘要: ").append(validationResult.getSummary()).append("\n");

        // 只筛选 ERROR（不包含 WARN）
        List<ValidationIssue> errors = validationResult.getIssues().stream()
                .filter(i -> "error".equalsIgnoreCase(i.getSeverity()))
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            promptBuilder.append("- ERROR 列表:\n");
            for (ValidationIssue error : errors) {
                promptBuilder.append("  - [").append(error.getRuleCode()).append("] ");
                if (StrUtil.isNotBlank(error.getFilePath())) {
                    promptBuilder.append("文件: ").append(error.getFilePath()).append(", ");
                }
                promptBuilder.append(error.getMessage()).append("\n");
            }
        }

        // 构建错误（仅 VUE_PROJECT）
        BuildValidationResult buildResult = validationResult.getBuildResult();
        if (buildResult != null && !buildResult.isOverallSuccess()) {
            promptBuilder.append("- 构建错误:\n");
            if (!buildResult.isInstallSuccess()) {
                promptBuilder.append("  - npm install 失败，退出码: ").append(buildResult.getInstallExitCode()).append("\n");
            }
            if (!buildResult.isBuildSuccess() && buildResult.isInstallSuccess()) {
                promptBuilder.append("  - npm run build 失败，退出码: ").append(buildResult.getBuildExitCode()).append("\n");
            }
            if (!buildResult.getKeyErrors().isEmpty()) {
                promptBuilder.append("  - 关键错误:\n");
                for (String keyError : buildResult.getKeyErrors()) {
                    promptBuilder.append("    - ").append(keyError).append("\n");
                }
            }
        }
        promptBuilder.append("\n");

        // 3. 修复要求
        promptBuilder.append("【修复要求】\n");
        promptBuilder.append("1. 只修复导致校验/构建失败的问题，不要修改其他正常代码\n");
        promptBuilder.append("2. 不要重构无关代码或文件\n");
        promptBuilder.append("3. 不要新增额外功能\n");
        promptBuilder.append("4. 尽量做最小改动\n");
        promptBuilder.append("5. 修复后的代码必须能通过校验和构建\n");
        promptBuilder.append("6. 保持代码风格一致\n\n");

        // 4. 当前代码
        promptBuilder.append("【当前代码】\n");
        appendCurrentCode(promptBuilder, codeGenType, appId, errors);

        // 5. 输出格式要求
        promptBuilder.append("\n【输出要求】\n");
        if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            promptBuilder.append("请使用 writeFile 工具修复相关文件。\n");
            promptBuilder.append("如果需要修改文件的部分内容，请使用 modifyFile 工具。\n");
        } else {
            promptBuilder.append("请输出修复后的完整代码。\n");
            if ("HTML".equalsIgnoreCase(codeGenType)) {
                promptBuilder.append("输出一个完整的 HTML 文件，包含修复后的内容。\n");
            } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType)) {
                promptBuilder.append("使用 --- 文件名: xxx --- 分隔符分隔不同文件。\n");
            }
        }

        return promptBuilder.toString();
    }

    /**
     * 根据项目类型添加当前代码内容
     */
    private void appendCurrentCode(StringBuilder promptBuilder, String codeGenType, Long appId, List<ValidationIssue> errors) {
        String projectPath = getProjectPath(codeGenType, appId);
        File projectDir = new File(projectPath);

        if (!projectDir.exists()) {
            promptBuilder.append("项目目录不存在: ").append(projectPath).append("\n");
            return;
        }

        if ("HTML".equalsIgnoreCase(codeGenType)) {
            // HTML 类型：提供完整 HTML 文件内容
            File indexHtml = new File(projectDir, "index.html");
            if (indexHtml.exists()) {
                String content = FileUtil.readString(indexHtml, StandardCharsets.UTF_8);
                promptBuilder.append("当前 HTML 文件内容:\n");
                promptBuilder.append(truncateContent(content)).append("\n");
            }
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType)) {
            // MULTI_FILE 类型：提供三个文件内容
            File indexHtml = new File(projectDir, "index.html");
            File styleCss = new File(projectDir, "style.css");
            File scriptJs = new File(projectDir, "script.js");

            if (indexHtml.exists()) {
                promptBuilder.append("index.html:\n");
                promptBuilder.append(truncateContent(FileUtil.readString(indexHtml, StandardCharsets.UTF_8))).append("\n");
            }
            if (styleCss.exists()) {
                promptBuilder.append("style.css:\n");
                promptBuilder.append(truncateContent(FileUtil.readString(styleCss, StandardCharsets.UTF_8))).append("\n");
            }
            if (scriptJs.exists()) {
                promptBuilder.append("script.js:\n");
                promptBuilder.append(truncateContent(FileUtil.readString(scriptJs, StandardCharsets.UTF_8))).append("\n");
            }
        } else if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            // VUE_PROJECT 类型：提供关键文件和报错涉及文件
            promptBuilder.append("项目结构:\n");
            promptBuilder.append("- package.json\n");
            promptBuilder.append("- vite.config.js\n");
            promptBuilder.append("- src/main.js 或 src/main.ts\n");
            promptBuilder.append("- src/App.vue\n");
            promptBuilder.append("- src/router/index.js（如有）\n");

            // 提取报错涉及的文件路径
            List<String> errorFilePaths = errors.stream()
                    .filter(e -> StrUtil.isNotBlank(e.getFilePath()))
                    .map(ValidationIssue::getFilePath)
                    .distinct()
                    .collect(Collectors.toList());

            if (!errorFilePaths.isEmpty()) {
                promptBuilder.append("\n报错涉及的文件:\n");
                for (String filePath : errorFilePaths) {
                    File errorFile = new File(projectDir, filePath);
                    if (errorFile.exists()) {
                        promptBuilder.append(filePath).append(":\n");
                        promptBuilder.append(truncateContent(FileUtil.readString(errorFile, StandardCharsets.UTF_8))).append("\n");
                    }
                }
            } else {
                // 如果没有明确报错文件，提供几个关键文件
                promptBuilder.append("\n关键文件内容:\n");
                appendVueKeyFiles(promptBuilder, projectDir);
            }
        }
    }

    /**
     * 添加 Vue 项目关键文件内容
     */
    private void appendVueKeyFiles(StringBuilder promptBuilder, File projectDir) {
        File srcDir = new File(projectDir, "src");
        if (!srcDir.exists()) {
            return;
        }

        // main.js 或 main.ts
        File mainJs = new File(srcDir, "main.js");
        File mainTs = new File(srcDir, "main.ts");
        if (mainJs.exists()) {
            promptBuilder.append("src/main.js:\n");
            promptBuilder.append(truncateContent(FileUtil.readString(mainJs, StandardCharsets.UTF_8))).append("\n");
        } else if (mainTs.exists()) {
            promptBuilder.append("src/main.ts:\n");
            promptBuilder.append(truncateContent(FileUtil.readString(mainTs, StandardCharsets.UTF_8))).append("\n");
        }

        // App.vue
        File appVue = new File(srcDir, "App.vue");
        if (appVue.exists()) {
            promptBuilder.append("src/App.vue:\n");
            promptBuilder.append(truncateContent(FileUtil.readString(appVue, StandardCharsets.UTF_8))).append("\n");
        }
    }

    /**
     * 获取项目路径
     */
    private String getProjectPath(String codeGenType, Long appId) {
        if ("VUE_PROJECT".equalsIgnoreCase(codeGenType) || "vue_project".equalsIgnoreCase(codeGenType)) {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
        } else if ("MULTI_FILE".equalsIgnoreCase(codeGenType)) {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/MULTI_FILE_" + appId;
        } else {
            return AppConstant.CODE_OUTPUT_ROOT_DIR + "/HTML_" + appId;
        }
    }

    /**
     * 截断内容，避免过长的 prompt
     */
    private String truncateContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() > MAX_FILE_CONTENT_LENGTH) {
            return content.substring(0, MAX_FILE_CONTENT_LENGTH) + "\n... (内容过长，已截断)";
        }
        return content;
    }
}