package com.zkf.aicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.manage.CosManager;
import com.zkf.aicodemother.service.ScreenshotService;
import com.zkf.aicodemother.service.ScreenshotTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Autowired(required = false)
    private CosManager cosManager;

    @Resource
    private ScreenshotTaskExecutor screenshotTaskExecutor;

    /**
     * 同步等待截图完成的超时时间（秒）
     */
    private static final int SYNC_TIMEOUT_SECONDS = 60;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);

        // 1. 异步执行截图任务
        CompletableFuture<String> future = screenshotTaskExecutor.executeAsync(webUrl);
        ThrowUtils.throwIf(future == null, ErrorCode.OPERATION_ERROR, "截图任务提交失败");

        try {
            // 2. 等待截图完成（同步等待）
            String localScreenshotPath = future.get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "本地截图生成失败");

            try {
                // 3. 上传到对象存储
                String cosUrl = uploadScreenshotToCos(localScreenshotPath);
                ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");
                log.info("网页截图生成并上传成功: {} -> {}", webUrl, cosUrl);
                return cosUrl;
            } finally {
                // 4. 清理本地文件
                cleanupLocalFile(localScreenshotPath);
            }
        } catch (Exception e) {
            log.error("截图任务执行异常: {}", webUrl, e);
            return null;
        }
    }

    /**
     * 异步生成并上传截图（不阻塞调用线程）
     *
     * @param webUrl 网页URL
     * @return CompletableFuture包含COS访问URL
     */
    public CompletableFuture<String> generateAndUploadScreenshotAsync(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return CompletableFuture.completedFuture(null);
        }
        log.info("异步生成网页截图，URL: {}", webUrl);

        return screenshotTaskExecutor.executeAsync(webUrl)
                .thenApply(localPath -> {
                    if (StrUtil.isBlank(localPath)) {
                        log.warn("截图生成失败: {}", webUrl);
                        return null;
                    }
                    try {
                        String cosUrl = uploadScreenshotToCos(localPath);
                        if (StrUtil.isNotBlank(cosUrl)) {
                            log.info("网页截图上传成功: {} -> {}", webUrl, cosUrl);
                        }
                        return cosUrl;
                    } finally {
                        cleanupLocalFile(localPath);
                    }
                })
                .exceptionally(e -> {
                    log.error("异步截图任务异常: {}", webUrl, e);
                    return null;
                });
    }

    /**
     * 上传截图到对象存储
     *
     * @param localScreenshotPath 本地截图路径
     * @return 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        if (cosManager == null || !cosManager.isConfigured()) {
            log.warn("COS未配置，截图将仅保存在本地: {}", localScreenshotPath);
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地文件
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        if (StrUtil.isBlank(localFilePath)) {
            return;
        }
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("本地截图文件已清理: {}", localFilePath);
        }
    }
}