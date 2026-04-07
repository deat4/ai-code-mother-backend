package com.zkf.aicodemother.config;

import cn.hutool.core.io.FileUtil;
import com.zkf.aicodemother.service.ScreenshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 截图相关定时任务配置
 */
@Configuration
@EnableScheduling
@Slf4j
public class ScreenshotConfig {

    /**
     * 临时截图目录
     */
    private static final String TEMP_SCREENSHOT_DIR = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots";

    /**
     * 临时文件过期时间（小时）
     */
    private static final int EXPIRE_HOURS = 24;

    /**
     * 每天凌晨2点清理过期的临时截图文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempScreenshots() {
        log.info("开始定时清理过期的临时截图文件");
        try {
            cleanupExpiredTempFiles();
            log.info("定时清理临时截图文件完成");
        } catch (Exception e) {
            log.error("定时清理临时截图文件失败", e);
        }
    }

    /**
     * 清理过期的临时文件
     * 删除创建时间超过 EXPIRE_HOURS 小时的文件
     */
    private void cleanupExpiredTempFiles() {
        File screenshotDir = new File(TEMP_SCREENSHOT_DIR);
        if (!screenshotDir.exists() || !screenshotDir.isDirectory()) {
            log.info("临时截图目录不存在: {}", TEMP_SCREENSHOT_DIR);
            return;
        }

        File[] subDirs = screenshotDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            log.info("临时截图目录为空");
            return;
        }

        Instant expireTime = Instant.now().minus(EXPIRE_HOURS, ChronoUnit.HOURS);
        int deletedCount = 0;

        for (File subDir : subDirs) {
            // 检查目录最后修改时间
            long lastModified = subDir.lastModified();
            if (lastModified < expireTime.toEpochMilli()) {
                FileUtil.del(subDir);
                deletedCount++;
                log.debug("已删除过期临时目录: {}", subDir.getAbsolutePath());
            }
        }

        log.info("清理完成，共删除 {} 个过期临时目录", deletedCount);
    }
}