package com.zkf.aicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 校验延迟配置
 * 仅在 dev/test profile 启用，用于测试取消场景和修复流程
 *
 * 安全说明：
 * - 默认关闭（enabled=false）
 * - 只有在 spring.profiles.active 包含 dev 或 test 时才会加载此配置
 * - 生产环境（location/prod 等）不会加载此 Bean
 */
@Data
@Configuration
@Profile({"dev", "test"})
@ConfigurationProperties(prefix = "test.validation-delay")
public class ValidationDelayConfig {

    /**
     * 是否启用延迟
     */
    private boolean enabled = false;

    /**
     * 校验前延迟（毫秒）
     * 用于测试在 VALIDATING 阶段取消的场景
     */
    private long beforeValidationDelayMs = 0;

    /**
     * 构建前延迟（毫秒）
     * 用于测试在 BUILDING 阶段取消的场景
     */
    private long beforeBuildDelayMs = 0;

    /**
     * 修复前延迟（毫秒）
     * 用于测试在 REPAIRING 阶段取消的场景
     */
    private long beforeRepairDelayMs = 0;

    /**
     * 截图前延迟（毫秒）
     * 用于测试在 SCREENSHOT 阶段取消的场景
     */
    private long beforeScreenshotDelayMs = 0;

    /**
     * 执行延迟（如果启用）
     *
     * @param delayType 延迟类型：validation, build, repair, screenshot
     */
    public void applyDelay(String delayType) {
        if (!enabled) {
            return;
        }

        long delayMs = switch (delayType.toLowerCase()) {
            case "validation" -> beforeValidationDelayMs;
            case "build" -> beforeBuildDelayMs;
            case "repair" -> beforeRepairDelayMs;
            case "screenshot" -> beforeScreenshotDelayMs;
            default -> 0;
        };

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 执行校验前延迟
     */
    public void applyValidationDelay() {
        applyDelay("validation");
    }

    /**
     * 执行构建前延迟
     */
    public void applyBuildDelay() {
        applyDelay("build");
    }

    /**
     * 执行修复前延迟
     */
    public void applyRepairDelay() {
        applyDelay("repair");
    }

    /**
     * 执行截图前延迟
     */
    public void applyScreenshotDelay() {
        applyDelay("screenshot");
    }
}