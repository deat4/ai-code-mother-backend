package com.zkf.aicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /**
     * 部署配置
     */
    private DeployConfig deploy = new DeployConfig();

    /**
     * 预览配置
     */
    private PreviewConfig preview = new PreviewConfig();

    @Data
    public static class DeployConfig {
        /**
         * 已部署应用访问域名
         */
        private String host = "http://localhost:8123/api/static";
    }

    @Data
    public static class PreviewConfig {
        /**
         * 预览应用访问域名
         */
        private String host = "http://localhost:8123/api/preview";
    }
}