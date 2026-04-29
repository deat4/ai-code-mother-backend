package com.zkf.aicodemother.core.validation.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationIssue;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.core.validation.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * MULTI_FILE 校验服务实现
 */
@Slf4j
@Service
public class MultiFileValidationService implements ValidationService {

    /**
     * 最小文件大小（字节）
     */
    private static final int MIN_FILE_SIZE = 50;

    /**
     * 最少有效文件数
     */
    private static final int MIN_VALID_FILES = 2;

    /**
     * Markdown 代码块模式
     */
    private static final Pattern MARKDOWN_BLOCK_PATTERN = Pattern.compile("```(html|css|javascript|js)\\s*|```\\s*", Pattern.MULTILINE);

    /**
     * 占位符模式
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "TODO|FIXME|your-api-key|YOUR_API_KEY|placeholder|\\[object Object\\]",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 资源引用模式（检测异常路径）
     */
    private static final Pattern INVALID_PATH_PATTERN = Pattern.compile(
            "(href|src)\\s*=\\s*\"(https?://localhost|file:///|undefined|null)\"",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public ValidationResult validate(ValidationContext context) {
        return validateMultiFile(context);
    }

    @Override
    public ValidationResult validateHtml(ValidationContext context) {
        throw new UnsupportedOperationException("请使用 HtmlValidationService 校验 HTML 类型");
    }

    @Override
    public ValidationResult validateMultiFile(ValidationContext context) {
        log.info("开始校验 MULTI_FILE 项目: appId={}", context.getAppId());

        ValidationResult result = ValidationResult.builder()
                .codeGenType("MULTI_FILE")
                .stage("VALIDATING")
                .passed(true)
                .issues(new ArrayList<>())
                .build();

        // 构建项目目录路径
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/MULTI_FILE_" + context.getAppId();
        File projectDir = new File(projectPath);

        // 1. 检查项目目录是否存在
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            result.setPassed(false);
            result.setSummary("项目目录不存在");
            result.addIssue(ValidationIssue.error("structure", "PROJECT_DIR_MISSING",
                    "项目目录不存在: " + projectPath));
            return result;
        }

        // 2. 检查关键入口文件
        File indexHtml = new File(projectDir, "index.html");
        File styleCss = new File(projectDir, "style.css");
        File scriptJs = new File(projectDir, "script.js");

        // index.html 必须存在
        if (!indexHtml.exists()) {
            result.setPassed(false);
            result.setSummary("入口文件 index.html 不存在");
            result.addIssue(ValidationIssue.error("structure", "INDEX_MISSING",
                    "入口文件 index.html 不存在"));
            return result;
        }

        // 3. 检查至少有 2 个有效文件
        List<File> validFiles = new ArrayList<>();
        for (File file : List.of(indexHtml, styleCss, scriptJs)) {
            if (file.exists() && file.length() > 0) {
                validFiles.add(file);
            }
        }

        if (validFiles.size() < MIN_VALID_FILES) {
            result.setPassed(false);
            result.setSummary("有效文件数量不足，至少需要 " + MIN_VALID_FILES + " 个文件");
            result.addIssue(ValidationIssue.error("structure", "FILES_COUNT_INSUFFICIENT",
                    "当前只有 " + validFiles.size() + " 个有效文件"));
            return result;
        }

        // 4. 检查各文件内容
        int totalSize = 0;
        for (File file : validFiles) {
            String content = FileUtil.readString(file, StandardCharsets.UTF_8);
            totalSize += content.length();

            // 检查是否为空文件
            if (StrUtil.isBlank(content) || content.length() < MIN_FILE_SIZE) {
                result.addIssue(ValidationIssue.warn("content", file.getName(), "FILE_TOO_SMALL",
                        "文件 " + file.getName() + " 内容过小"));
            }

            // 检查是否残留 markdown 代码块
            if (MARKDOWN_BLOCK_PATTERN.matcher(content).find()) {
                result.addIssue(ValidationIssue.warn("content", file.getName(), "MARKDOWN_REMAINED",
                        "文件 " + file.getName() + " 中残留 markdown 代码块标记"));
            }

            // 检查占位符
            if (PLACEHOLDER_PATTERN.matcher(content).find()) {
                result.addIssue(ValidationIssue.warn("content", file.getName(), "PLACEHOLDER_FOUND",
                        "文件 " + file.getName() + " 中存在占位符"));
            }
        }

        // 5. 检查 HTML 中的资源引用路径
        String htmlContent = FileUtil.readString(indexHtml, StandardCharsets.UTF_8);
        if (INVALID_PATH_PATTERN.matcher(htmlContent).find()) {
            result.addIssue(ValidationIssue.warn("content", "index.html", "INVALID_PATH_REFERENCE",
                    "HTML 中存在异常的资源引用路径"));
        }

        // 设置结果摘要（统一语义：passed 只看 ERROR，警告体现在摘要中）
        if (result.isPassedByErrors()) {
            int warningCount = result.getWarningCount();
            if (warningCount > 0) {
                result.setSummary(String.format("MULTI_FILE 校验通过，存在 %d 个警告", warningCount));
            } else {
                result.setSummary("MULTI_FILE 校验通过");
            }
            result.getExtra().put("fileCount", validFiles.size());
            result.getExtra().put("totalSize", totalSize);
            result.getExtra().put("projectPath", projectPath);
        }

        log.info("MULTI_FILE 校验完成: appId={}, passed={}, errorCount={}, warningCount={}",
                context.getAppId(), result.isPassedByErrors(), result.getErrorCount(), result.getWarningCount());

        return result;
    }

    @Override
    public ValidationResult validateVueProject(ValidationContext context) {
        throw new UnsupportedOperationException("请使用 VueProjectValidationService 校验 VUE_PROJECT 类型");
    }

    @Override
    public BuildValidationResult validateVueBuild(ValidationContext context) {
        return null;
    }
}