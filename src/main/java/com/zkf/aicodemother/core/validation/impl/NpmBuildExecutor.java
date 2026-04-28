package com.zkf.aicodemother.core.validation.impl;

import cn.hutool.core.util.RuntimeUtil;
import com.zkf.aicodemother.core.validation.BuildExecutor;
import com.zkf.aicodemother.core.validation.BuildValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NPM 构建执行器
 * 执行 npm install 和 npm run build，返回结构化结果
 */
@Slf4j
@Component
public class NpmBuildExecutor implements BuildExecutor {

    /**
     * npm install 超时时间（秒）
     */
    private static final int NPM_INSTALL_TIMEOUT = 300;

    /**
     * npm run build 超时时间（秒）
     */
    private static final int NPM_BUILD_TIMEOUT = 180;

    /**
     * 关键错误模式（用于从日志中提取关键错误）
     */
    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "(Error:|ERROR|Cannot|Failed|failed|Module not found|SyntaxError|TypeError)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public BuildValidationResult executeBuild(Path projectDir) {
        return executeBuild(projectDir.toString());
    }

    @Override
    public BuildValidationResult executeBuild(String projectPath) {
        log.info("开始执行 NPM 构建: {}", projectPath);

        BuildValidationResult result = BuildValidationResult.builder()
                .keyErrors(new ArrayList<>())
                .build();

        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            result.setInstallSuccess(false);
            result.setBuildSuccess(false);
            result.addKeyError("项目目录不存在: " + projectPath);
            return result;
        }

        // 1. 执行 npm install
        long installStart = System.currentTimeMillis();
        ExecuteResult installResult = executeNpmInstall(projectDir);
        long installEnd = System.currentTimeMillis();

        result.setInstallSuccess(installResult.success);
        result.setInstallExitCode(installResult.exitCode);
        result.setInstallLog(installResult.output);
        result.setInstallDurationMs(installEnd - installStart);

        if (!installResult.success) {
            result.setBuildSuccess(false);
            result.addKeyError("npm install 失败，退出码: " + installResult.exitCode);
            extractKeyErrors(installResult.output, result.getKeyErrors());
            log.error("npm install 失败: {}", projectPath);
            return result;
        }

        log.info("npm install 成功: {}", projectPath);

        // 2. 执行 npm run build
        long buildStart = System.currentTimeMillis();
        ExecuteResult buildResult = executeNpmBuild(projectDir);
        long buildEnd = System.currentTimeMillis();

        result.setBuildSuccess(buildResult.success);
        result.setBuildExitCode(buildResult.exitCode);
        result.setBuildLog(buildResult.output);
        result.setBuildDurationMs(buildEnd - buildStart);

        if (!buildResult.success) {
            result.addKeyError("npm run build 失败，退出码: " + buildResult.exitCode);
            extractKeyErrors(buildResult.output, result.getKeyErrors());
            log.error("npm run build 失败: {}", projectPath);
            return result;
        }

        // 3. 检查 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            result.setBuildSuccess(false);
            result.addKeyError("构建完成但 dist 目录未生成");
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return result;
        }

        log.info("NPM 构建成功: {}, 耗时: install={}ms, build={}ms",
                projectPath, result.getInstallDurationMs(), result.getBuildDurationMs());

        return result;
    }

    /**
     * 执行 npm install
     */
    private ExecuteResult executeNpmInstall(File projectDir) {
        String command = buildNpmCommand("install");
        return executeCommand(projectDir, command, NPM_INSTALL_TIMEOUT);
    }

    /**
     * 执行 npm run build
     */
    private ExecuteResult executeNpmBuild(File projectDir) {
        String command = buildNpmCommand("run build");
        return executeCommand(projectDir, command, NPM_BUILD_TIMEOUT);
    }

    /**
     * 执行命令并收集输出
     */
    private ExecuteResult executeCommand(File workingDir, String command, int timeoutSeconds) {
        ExecuteResult result = new ExecuteResult();

        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);

            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.directory(workingDir);
            pb.redirectErrorStream(true); // 合并 stdout 和 stderr

            Process process = pb.start();

            // 读取输出
            StringBuilder outputBuilder = new StringBuilder();
            try (var reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程: {}", timeoutSeconds, command);
                process.destroyForcibly();
                result.success = false;
                result.exitCode = -1;
                result.output = outputBuilder.toString() + "\n[TIMEOUT] 命令执行超时";
                return result;
            }

            result.exitCode = process.exitValue();
            result.output = outputBuilder.toString();
            result.success = result.exitCode == 0;

            if (!result.success) {
                log.error("命令执行失败，退出码: {}, 命令: {}", result.exitCode, command);
            }

        } catch (Exception e) {
            log.error("执行命令异常: {}, 错误: {}", command, e.getMessage(), e);
            result.success = false;
            result.exitCode = -1;
            result.output = "执行异常: " + e.getMessage();
        }

        return result;
    }

    /**
     * 根据操作系统构建 npm 命令
     */
    private String buildNpmCommand(String subCommand) {
        String npm = isWindows() ? "npm.cmd" : "npm";
        return npm + " " + subCommand;
    }

    /**
     * 判断是否为 Windows 系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 从日志中提取关键错误
     */
    private void extractKeyErrors(String log, List<String> keyErrors) {
        if (log == null || log.isEmpty()) {
            return;
        }

        // 提取包含错误关键词的行
        String[] lines = log.split("\n");
        for (String line : lines) {
            Matcher matcher = ERROR_PATTERN.matcher(line);
            if (matcher.find()) {
                // 限制每行长度，避免过长
                String trimmedLine = line.trim();
                if (trimmedLine.length() > 200) {
                    trimmedLine = trimmedLine.substring(0, 200) + "...";
                }
                keyErrors.add(trimmedLine);
            }
        }

        // 最多保留 10 条关键错误
        if (keyErrors.size() > 10) {
            keyErrors = new ArrayList<>(keyErrors.subList(0, 10));
        }
    }

    /**
     * 命令执行结果内部类
     */
    private static class ExecuteResult {
        boolean success;
        int exitCode;
        String output;
    }
}