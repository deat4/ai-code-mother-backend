package com.zkf.aicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 失败注入配置
 * 仅在 dev/test profile 启用，用于测试自动修复流程
 *
 * 安全说明：
 * - 默认关闭（enabled=false）
 * - 只有在 spring.profiles.active 包含 dev 或 test 时才会加载此配置
 * - 生产环境（location/prod 等）不会加载此 Bean
 */
@Data
@Configuration
@Profile({"dev", "test"})
@ConfigurationProperties(prefix = "test.failure-injection")
public class FailureInjectionConfig {

    /**
     * 是否启用失败注入
     */
    private boolean enabled = false;

    /**
     * HTML 类型失败模式
     * - MISSING_INDEX: 缺少 index.html
     * - EMPTY_FILE: 空文件
     * - INVALID_HTML: 无效 HTML（缺少关键标签）
     * - NONE: 不注入失败
     */
    private String htmlFailureMode = "NONE";

    /**
     * MULTI_FILE 类型失败模式
     * - MISSING_SCRIPT: 缺少 script.js
     * - MISSING_STYLE: 缺少 style.css
     * - EMPTY_HTML: 空的 index.html
     * - NONE: 不注入失败
     */
    private String multiFileFailureMode = "NONE";

    /**
     * VUE_PROJECT 类型失败模式
     * - MISSING_MAIN_JS: 缺少 main.js
     * - MISSING_PACKAGE_JSON: 缺少 package.json
     * - BUILD_FAIL: 强制构建失败（npm run build 返回错误）
     * - SYNTAX_ERROR: 在关键文件中注入语法错误
     * - NONE: 不注入失败
     */
    private String vueProjectFailureMode = "NONE";

    /**
     * 是否启用构建失败注入
     */
    private boolean forceBuildFail = false;

    /**
     * 构建失败退出码
     */
    private int buildFailExitCode = 1;

    /**
     * 构建失败错误消息
     */
    private String buildFailMessage = "模拟构建失败";

    /**
     * 判断是否需要注入 HTML 失败
     */
    public boolean shouldInjectHtmlFailure() {
        return enabled && !"NONE".equalsIgnoreCase(htmlFailureMode);
    }

    /**
     * 判断是否需要注入 MULTI_FILE 失败
     */
    public boolean shouldInjectMultiFileFailure() {
        return enabled && !"NONE".equalsIgnoreCase(multiFileFailureMode);
    }

    /**
     * 判断是否需要注入 VUE_PROJECT 失败
     */
    public boolean shouldInjectVueProjectFailure() {
        return enabled && !"NONE".equalsIgnoreCase(vueProjectFailureMode);
    }
}