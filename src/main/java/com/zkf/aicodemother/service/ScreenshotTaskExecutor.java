package com.zkf.aicodemother.service;

import java.util.concurrent.CompletableFuture;

/**
 * 截图任务执行器接口
 * 支持多种实现：本地单线程、Kafka消息队列等
 */
public interface ScreenshotTaskExecutor {

    /**
     * 异步执行截图任务
     *
     * @param webUrl 网页URL
     * @return CompletableFuture包含截图URL，失败返回null
     */
    CompletableFuture<String> executeAsync(String webUrl);

    /**
     * 获取执行器名称（用于日志和监控）
     *
     * @return 执行器名称
     */
    String getExecutorName();
}