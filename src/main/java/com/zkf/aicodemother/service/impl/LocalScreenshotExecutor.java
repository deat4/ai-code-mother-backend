package com.zkf.aicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.service.ScreenshotTaskExecutor;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * 本地截图任务执行器（单线程串行执行）
 * 使用单线程执行器确保所有截图任务串行执行，避免 WebDriver 并发问题
 */
@Slf4j
@Component
public class LocalScreenshotExecutor implements ScreenshotTaskExecutor {

    /**
     * 单线程执行器，确保截图任务串行执行
     */
    private final ExecutorService executor;

    /**
     * 共享的 WebDriver 实例
     */
    private volatile WebDriver webDriver;

    /**
     * WebDriver 是否初始化成功
     */
    private volatile boolean webDriverInitialized = false;

    /**
     * 单个任务超时时间（秒）
     */
    private static final int TASK_TIMEOUT_SECONDS = 60;

    /**
     * 页面加载超时时间（秒）
     */
    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 30;

    /**
     * 默认窗口宽度
     */
    private static final int DEFAULT_WIDTH = 1600;

    /**
     * 默认窗口高度
     */
    private static final int DEFAULT_HEIGHT = 900;

    public LocalScreenshotExecutor() {
        // 创建单线程执行器，确保任务串行执行
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "screenshot-executor");
            t.setDaemon(true);  // 守护线程，不阻塞 JVM 退出
            return t;
        });
        // 延迟初始化 WebDriver，不在构造函数中初始化
        log.info("LocalScreenshotExecutor 初始化完成（WebDriver 延迟初始化）");
    }

    /**
     * 检查截图功能是否可用
     */
    public boolean isAvailable() {
        return webDriverInitialized && isWebDriverHealthy();
    }

    @Override
    public CompletableFuture<String> executeAsync(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return CompletableFuture.completedFuture(null);
        }

        // 检查 WebDriver 是否可用
        if (!webDriverInitialized) {
            synchronized (this) {
                if (!webDriverInitialized) {
                    try {
                        initWebDriver();
                    } catch (Exception e) {
                        log.error("WebDriver 初始化失败，截图功能不可用", e);
                        return CompletableFuture.completedFuture(null);
                    }
                }
            }
        }

        log.info("提交截图任务: {}", webUrl);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return doScreenshot(webUrl);
            } catch (Exception e) {
                log.error("截图任务执行失败: {}", webUrl, e);
                return null;
            }
        }, executor).orTimeout(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .exceptionally(e -> {
              log.error("截图任务超时或异常: {}", webUrl, e);
              return null;
          });
    }

    @Override
    public String getExecutorName() {
        return "LocalScreenshotExecutor";
    }

    /**
     * 执行实际截图操作
     *
     * @param webUrl 网页URL
     * @return 截图文件路径
     */
    private String doScreenshot(String webUrl) {
        try {
            // 检查 WebDriver 是否健康
            if (!isWebDriverHealthy()) {
                log.warn("WebDriver 不健康，尝试重建...");
                rebuildWebDriver();
            }

            // 创建临时目录
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            // 原始截图文件路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + ".png";

            // 访问网页
            webDriver.get(webUrl);

            // 等待页面加载完成
            waitForPageLoad(webDriver);

            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);

            // 压缩图片
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);

            // 删除原始图片
            FileUtil.del(imageSavePath);

            return compressedImagePath;
        } catch (Exception e) {
            log.error("截图执行失败: {}", webUrl, e);
            // 截图失败时，尝试重建 WebDriver
            try {
                rebuildWebDriver();
            } catch (Exception ex) {
                log.error("重建 WebDriver 失败", ex);
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "截图失败: " + e.getMessage());
        }
    }

    /**
     * 初始化 WebDriver
     */
    private synchronized void initWebDriver() {
        if (webDriverInitialized) {
            return;
        }
        try {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments(String.format("--window-size=%d,%d", DEFAULT_WIDTH, DEFAULT_HEIGHT));
            options.addArguments("--disable-extensions");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            webDriver = new ChromeDriver(options);
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS));
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            webDriverInitialized = true;
            log.info("WebDriver 初始化成功");
        } catch (Exception e) {
            log.error("WebDriver 初始化失败: {}", e.getMessage());
            webDriverInitialized = false;
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "WebDriver 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 检查 WebDriver 是否健康
     */
    private boolean isWebDriverHealthy() {
        try {
            if (webDriver == null || !webDriverInitialized) {
                return false;
            }
            // 尝试获取当前 URL 来检查 WebDriver 是否响应
            webDriver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            log.warn("WebDriver 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 重建 WebDriver
     */
    private synchronized void rebuildWebDriver() {
        log.info("开始重建 WebDriver...");
        try {
            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (Exception e) {
                    log.warn("关闭旧 WebDriver 失败", e);
                }
            }
            webDriverInitialized = false;
            initWebDriver();
            log.info("WebDriver 重建成功");
        } catch (Exception e) {
            log.error("WebDriver 重建失败", e);
            webDriverInitialized = false;
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "WebDriver 重建失败");
        }
    }

    /**
     * 保存图片到文件
     */
    private void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     */
    private void compressImage(String originalImagePath, String compressedImagePath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成
     */
    private void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待动态内容加载
            Thread.sleep(2000);
            log.debug("页面加载完成");
        } catch (Exception e) {
            log.warn("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        log.info("开始关闭 LocalScreenshotExecutor...");

        // 关闭执行器
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭 WebDriver
        if (webDriver != null) {
            try {
                webDriver.quit();
                log.info("WebDriver 已关闭");
            } catch (Exception e) {
                log.warn("关闭 WebDriver 失败", e);
            }
        }

        log.info("LocalScreenshotExecutor 已关闭");
    }
}