package com.zkf.aicodemother.core.validation.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.core.validation.BuildExecutor;
import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationIssue;
import com.zkf.aicodemother.core.validation.ValidationResult;
import com.zkf.aicodemother.core.validation.ValidationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * VUE_PROJECT 校验服务实现
 * 静态结构校验 + 调构建服务
 */
@Slf4j
@Service
public class VueProjectValidationService implements ValidationService {

    private static final int MAX_FILE_COUNT = 200;
    private static final long MAX_CODE_SIZE = 1024 * 1024 * 2;

    @Resource
    private BuildExecutor buildExecutor;

    @Override
    public ValidationResult validate(ValidationContext context) {
        return validateVueProject(context);
    }

    @Override
    public ValidationResult validateHtml(ValidationContext context) {
        throw new UnsupportedOperationException("请使用 HtmlValidationService 校验 HTML 类型");
    }

    @Override
    public ValidationResult validateMultiFile(ValidationContext context) {
        throw new UnsupportedOperationException("请使用 MultiFileValidationService 校验 MULTI_FILE 类型");
    }

    @Override
    public ValidationResult validateVueProject(ValidationContext context) {
        log.info("开始校验 VUE_PROJECT 结构: appId={}", context.getAppId());

        ValidationResult result = ValidationResult.builder()
                .codeGenType("VUE_PROJECT")
                .stage("VALIDATING")
                .passed(true)
                .issues(new ArrayList<>())
                .build();

        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + context.getAppId();
        File projectDir = new File(projectPath);

        result.getExtra().put("projectPath", projectPath);

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            result.setPassed(false);
            result.setSummary("项目目录不存在");
            result.addIssue(ValidationIssue.error("structure", "PROJECT_DIR_MISSING", "项目目录不存在: " + projectPath));
            return result;
        }

        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            result.setPassed(false);
            result.setSummary("package.json 不存在");
            result.addIssue(ValidationIssue.error("structure", "PACKAGE_JSON_MISSING", "package.json 不存在"));
            return result;
        }

        try {
            String packageContent = FileUtil.readString(packageJson, "UTF-8");
            JSONObject packageObj = JSONUtil.parseObj(packageContent);
            if (!packageObj.containsKey("scripts") || !packageObj.getJSONObject("scripts").containsKey("build")) {
                result.setPassed(false);
                result.setSummary("package.json 缺少 build 脚本");
                result.addIssue(ValidationIssue.error("config", "package.json", "BUILD_SCRIPT_MISSING", "package.json 缺少 build 脚本配置"));
                return result;
            }
            result.getExtra().put("packageName", packageObj.getStr("name"));
        } catch (Exception e) {
            result.setPassed(false);
            result.setSummary("package.json 解析失败");
            result.addIssue(ValidationIssue.error("config", "package.json", "PACKAGE_JSON_PARSE_ERROR", "package.json 解析失败: " + e.getMessage()));
            return result;
        }

        File srcDir = new File(projectDir, "src");
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            result.setPassed(false);
            result.setSummary("src 目录不存在");
            result.addIssue(ValidationIssue.error("structure", "SRC_DIR_MISSING", "src 目录不存在"));
            return result;
        }

        File mainJs = new File(srcDir, "main.js");
        File mainTs = new File(srcDir, "main.ts");
        if (!mainJs.exists() && !mainTs.exists()) {
            result.setPassed(false);
            result.setSummary("入口文件 main.js/main.ts 不存在");
            result.addIssue(ValidationIssue.error("structure", "MAIN_ENTRY_MISSING", "src/main.js 或 src/main.ts 不存在"));
            return result;
        }

        File appVue = new File(srcDir, "App.vue");
        if (!appVue.exists()) {
            result.setPassed(false);
            result.setSummary("App.vue 不存在");
            result.addIssue(ValidationIssue.error("structure", "APP_VUE_MISSING", "src/App.vue 不存在"));
            return result;
        }

        if (result.isPassedByErrors()) {
            int warningCount = result.getWarningCount();
            if (warningCount > 0) {
                result.setSummary(String.format("VUE_PROJECT 结构校验通过，存在 %d 个警告", warningCount));
            } else {
                result.setSummary("VUE_PROJECT 结构校验通过");
            }
        }

        log.info("VUE_PROJECT 结构校验完成: appId={}, passed={}, errorCount={}, warningCount={}",
                context.getAppId(), result.isPassedByErrors(), result.getErrorCount(), result.getWarningCount());
        return result;
    }

    @Override
    public BuildValidationResult validateVueBuild(ValidationContext context) {
        log.info("开始校验 VUE_PROJECT 构建: appId={}", context.getAppId());
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + context.getAppId();
        BuildValidationResult buildResult = buildExecutor.executeBuild(projectPath);
        log.info("VUE_PROJECT 构建完成: appId={}, installSuccess={}, buildSuccess={}", context.getAppId(), buildResult.isInstallSuccess(), buildResult.isBuildSuccess());
        return buildResult;
    }

    public ValidationResult validateFull(ValidationContext context) {
        ValidationResult structureResult = validateVueProject(context);
        if (!structureResult.isPassed()) {
            return structureResult;
        }

        BuildValidationResult buildResult = validateVueBuild(context);

        ValidationResult finalResult = ValidationResult.builder()
                .codeGenType("VUE_PROJECT")
                .stage("BUILDING")
                .passed(buildResult.isOverallSuccess())
                .issues(structureResult.getIssues())
                .buildResult(buildResult)
                .extra(structureResult.getExtra())
                .build();

        if (buildResult.isOverallSuccess()) {
            finalResult.setSummary("VUE_PROJECT 校验通过，构建成功");
        } else {
            finalResult.setSummary("构建失败: " + (buildResult.isInstallSuccess() ? "npm run build 失败" : "npm install 失败"));
            if (!buildResult.isInstallSuccess()) {
                finalResult.addIssue(ValidationIssue.builder()
                        .type("build")
                        .severity("error")
                        .ruleCode("NPM_INSTALL_FAILED")
                        .message("npm install 失败，退出码: " + buildResult.getInstallExitCode())
                        .suggestion("检查 package.json 依赖配置")
                        .build());
            }
            if (!buildResult.isBuildSuccess() && buildResult.isInstallSuccess()) {
                finalResult.addIssue(ValidationIssue.builder()
                        .type("build")
                        .severity("error")
                        .ruleCode("NPM_BUILD_FAILED")
                        .message("npm run build 失败，退出码: " + buildResult.getBuildExitCode())
                        .suggestion("检查代码语法和构建配置")
                        .build());
                for (String keyError : buildResult.getKeyErrors()) {
                    finalResult.addIssue(ValidationIssue.builder()
                            .type("build")
                            .severity("error")
                            .ruleCode("BUILD_ERROR")
                            .message(keyError)
                            .build());
                }
            }
        }
        return finalResult;
    }
}