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
 * HTML 校验服务实现
 */
@Slf4j
@Service
public class HtmlValidationService implements ValidationService {

    /**
     * 最小文件大小（字节）
     */
    private static final int MIN_FILE_SIZE = 100;

    /**
     * Markdown 代码块模式
     */
    private static final Pattern MARKDOWN_BLOCK_PATTERN = Pattern.compile("```html\\s*|```\\s*", Pattern.MULTILINE);

    /**
     * 占位符模式
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "TODO|FIXME|your-api-key|YOUR_API_KEY|placeholder|\\[object Object\\]",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 必要 HTML 标签模式
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<html", Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_TAG_PATTERN = Pattern.compile("<body", Pattern.CASE_INSENSITIVE);

    @Override
    public ValidationResult validate(ValidationContext context) {
        return validateHtml(context);
    }

    @Override
    public ValidationResult validateHtml(ValidationContext context) {
        log.info("开始校验 HTML 项目: appId={}", context.getAppId());

        ValidationResult result = ValidationResult.builder()
                .codeGenType("HTML")
                .stage("VALIDATING")
                .passed(true)
                .issues(new ArrayList<>())
                .build();

        // 构建项目目录路径
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/HTML_" + context.getAppId();
        File projectDir = new File(projectPath);

        // 1. 检查项目目录是否存在
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            result.setPassed(false);
            result.setSummary("项目目录不存在");
            result.addIssue(ValidationIssue.error("structure", "PROJECT_DIR_MISSING",
                    "项目目录不存在: " + projectPath));
            return result;
        }

        // 2. 检查 index.html 是否存在
        File indexFile = new File(projectDir, "index.html");
        if (!indexFile.exists()) {
            result.setPassed(false);
            result.setSummary("入口文件 index.html 不存在");
            result.addIssue(ValidationIssue.error("structure", "INDEX_MISSING",
                    "入口文件 index.html 不存在"));
            return result;
        }

        // 3. 读取文件内容
        String content = FileUtil.readString(indexFile, StandardCharsets.UTF_8);

        // 4. 检查文件是否为空或过小
        if (StrUtil.isBlank(content) || content.length() < MIN_FILE_SIZE) {
            result.setPassed(false);
            result.setSummary("index.html 文件内容为空或过小");
            result.addIssue(ValidationIssue.error("content", "FILE_EMPTY",
                    "文件内容为空或小于 " + MIN_FILE_SIZE + " 字节"));
            return result;
        }

        // 5. 检查是否残留 markdown 代码块
        if (MARKDOWN_BLOCK_PATTERN.matcher(content).find()) {
            result.addIssue(ValidationIssue.warn("content", "MARKDOWN_REMAINED",
                    "文件中残留 markdown 代码块标记，建议清理"));
            // 不标记为失败，只是警告，最终摘要会在结尾统一设置
        }

        // 6. 检查是否存在明显占位符
        if (PLACEHOLDER_PATTERN.matcher(content).find()) {
            result.addIssue(ValidationIssue.warn("content", "PLACEHOLDER_FOUND",
                    "文件中存在占位符文本，建议替换为实际内容"));
            // 不标记为失败，只是警告
        }

        // 7. 检查必要 HTML 标签是否存在
        if (!HTML_TAG_PATTERN.matcher(content).find()) {
            result.setPassed(false);
            result.setSummary("HTML 结构不完整，缺少 <html> 标签");
            result.addIssue(ValidationIssue.error("structure", "HTML_TAG_MISSING",
                    "缺少 <html> 标签"));
            return result;
        }

        if (!BODY_TAG_PATTERN.matcher(content).find()) {
            result.setPassed(false);
            result.setSummary("HTML 结构不完整，缺少 <body> 标签");
            result.addIssue(ValidationIssue.error("structure", "BODY_TAG_MISSING",
                    "缺少 <body> 标签"));
            return result;
        }

        // 设置成功摘要（统一语义：如果有警告，摘要要体现）
        if (result.isPassedByErrors()) {
            int warningCount = result.getWarningCount();
            if (warningCount > 0) {
                result.setSummary(String.format("HTML 校验通过，存在 %d 个警告", warningCount));
            } else {
                result.setSummary("HTML 校验通过");
            }
            result.getExtra().put("fileSize", content.length());
            result.getExtra().put("filePath", indexFile.getAbsolutePath());
        }

        log.info("HTML 校验完成: appId={}, passed={}, errorCount={}, warningCount={}",
                context.getAppId(), result.isPassedByErrors(), result.getErrorCount(), result.getWarningCount());

        return result;
    }

    @Override
    public ValidationResult validateMultiFile(ValidationContext context) {
        // HTML 校验服务不处理 MULTI_FILE
        throw new UnsupportedOperationException("请使用 MultiFileValidationService 校验 MULTI_FILE 类型");
    }

    @Override
    public ValidationResult validateVueProject(ValidationContext context) {
        // HTML 校验服务不处理 VUE_PROJECT
        throw new UnsupportedOperationException("请使用 VueProjectValidationService 校验 VUE_PROJECT 类型");
    }

    @Override
    public BuildValidationResult validateVueBuild(ValidationContext context) {
        // HTML 不需要构建
        return null;
    }
}