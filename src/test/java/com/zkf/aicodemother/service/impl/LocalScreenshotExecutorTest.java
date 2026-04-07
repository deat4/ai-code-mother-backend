package com.zkf.aicodemother.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Slf4j
@SpringBootTest
public class LocalScreenshotExecutorTest {

    @Resource
    private LocalScreenshotExecutor screenshotExecutor;

    @Test
    void testExecuteAsync() {
        String testUrl = "https://www.baidu.com";
        
        CompletableFuture<String> future = screenshotExecutor.executeAsync(testUrl);
        Assertions.assertNotNull(future);
        
        String result = future.join();
        log.info("截图保存路径: {}", result);
        Assertions.assertNotNull(result);
    }

    @Test
    void testMultipleScreenshots() {
        // 测试多个截图任务串行执行
        String[] urls = {
            "https://www.baidu.com",
            "https://www.bing.com"
        };

        CompletableFuture<String>[] futures = new CompletableFuture[urls.length];
        
        for (int i = 0; i < urls.length; i++) {
            futures[i] = screenshotExecutor.executeAsync(urls[i]);
            log.info("提交任务 {}: {}", i, urls[i]);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();

        for (int i = 0; i < futures.length; i++) {
            String result = futures[i].join();
            log.info("任务 {} 结果: {}", i, result);
            Assertions.assertNotNull(result);
        }
    }
}