package com.zkf.aicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import com.zkf.aicodemother.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Vue 项目构建器
 * 负责执行 npm install 和 npm run build 命令
 *
 * @author zkf
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * npm install 超时时间（秒）
     */
    private static final int NPM_INSTALL_TIMEOUT = 300;

    /**
     * npm run build 超时时间（秒）
     */
    private static final int NPM_BUILD_TIMEOUT = 180;

    /**
     * 异步构建项目（不阻塞主流程）
     * 使用 Java 21 虚拟线程执行构建
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的虚拟线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: projectPath={}, error={}", projectPath, e.getMessage(), e);
            }
        });
    }

    /**
     * 根据 appId 异步构建项目
     *
     * @param appId 应用ID
     */
    public void buildProjectAsyncByAppId(Long appId) {
        if (appId == null) {
            log.warn("appId 为空，跳过构建");
            return;
        }
        // 使用与 FileWriteTool 一致的前缀：vue_project_
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
        buildProjectAsync(projectPath);
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }

        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }

        log.info("开始构建 Vue 项目: {}", projectPath);

        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败，项目路径: {}", projectPath);
            return false;
        }

        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败，项目路径: {}", projectPath);
            return false;
        }

        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }

        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 执行 npm install 命令
     *
     * @param projectDir 项目目录
     * @return 是否执行成功
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, NPM_INSTALL_TIMEOUT);
    }

    /**
     * 执行 npm run build 命令
     *
     * @param projectDir 项目目录
     * @return 是否执行成功
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, NPM_BUILD_TIMEOUT);
    }

    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );

            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程: {}", timeoutSeconds, command);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                // 读取错误输出
                String errorOutput = RuntimeUtil.getErrorResult(process);
                log.error("命令执行失败，退出码: {}, 错误输出: {}", exitCode, errorOutput);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据操作系统构建命令
     * Windows 系统需要添加 .cmd 后缀
     *
     * @param baseCommand 基础命令
     * @return 完整命令
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 判断是否为 Windows 系统
     *
     * @return 是否为 Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}