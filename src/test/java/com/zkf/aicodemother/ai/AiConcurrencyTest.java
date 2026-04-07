package com.zkf.aicodemother.ai;

import com.zkf.aicodemother.config.AiCodeGeneratorServiceFactory;
import com.zkf.aicodemother.service.AiCodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 并发调用测试
 * 验证多例模式解决并发阻塞问题的效果
 *
 * 问题：StreamingChatModel 底层 HTTP 客户端是同步阻塞的
 * 解决方案：使用 Spring 多例模式，每次创建 AI 服务时获取新的 StreamingChatModel 实例
 */
@Slf4j
@SpringBootTest
class AiConcurrencyTest {

    @Resource
    private AiCodeGeneratorServiceFactory aiServiceFactory;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 测试多例模式解决并发阻塞问题
     * 预期：各请求的第一个响应时间应该接近
     */
    @Test
    void testConcurrencyWithPrototypePattern() throws InterruptedException {
        int concurrentRequests = 3;
        CountDownLatch latch = new CountDownLatch(concurrentRequests);

        log.info("========== 测试：多例模式（预期并行）==========");
        log.info("同时发起 {} 个并发请求", concurrentRequests);

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i + 1;
            final long appId = requestIndex * 1000L; // 使用不同的 appId 避免缓存冲突

            new Thread(() -> executeRequest(requestIndex, appId, testStartTime, latch)).start();
        }

        latch.await(120, java.util.concurrent.TimeUnit.SECONDS);

        long testEndTime = System.currentTimeMillis();
        log.info("========== 测试结束 ==========");
        log.info("总测试时间: {}ms", testEndTime - testStartTime);

        log.info("\n========== 分析结果 ==========");
        log.info("如果是并行执行：各请求的第一个响应时间应该接近（差异 < 1000ms）");
        log.info("如果是串行执行：后续请求的第一个响应时间会显著延迟（累加）");
    }

    /**
     * 执行单个 AI 请求
     */
    private void executeRequest(int requestIndex, long appId, long testStartTime, CountDownLatch latch) {
        try {
            long requestStartTime = System.currentTimeMillis();
            log.info("[请求-{}] 开始时间: {}, 距测试开始: {}ms, 线程: {}",
                    requestIndex,
                    LocalDateTime.now().format(TIME_FORMATTER),
                    requestStartTime - testStartTime,
                    Thread.currentThread().getName());

            // 获取 AI 服务（每次都是新实例，包含新的 StreamingChatModel）
            AiCodeGeneratorService aiService = aiServiceFactory.getAiCodeGeneratorService(appId);

            String prompt = String.format("请生成一个简单的标题：请求%d的测试页面", requestIndex);
            Flux<String> responseFlux = aiService.generateHtmlCodeStream(prompt);

            AtomicInteger chunkCount = new AtomicInteger(0);
            final long[] firstChunkTime = {0};

            responseFlux
                    .doOnNext(chunk -> {
                        if (chunkCount.incrementAndGet() == 1) {
                            firstChunkTime[0] = System.currentTimeMillis();
                            log.info("[请求-{}] 第一个响应到达，耗时: {}ms",
                                    requestIndex, firstChunkTime[0] - requestStartTime);
                        }
                    })
                    .doOnComplete(() -> {
                        long endTime = System.currentTimeMillis();
                        log.info("[请求-{}] 完成时间: {}, 总耗时: {}ms, 响应块数: {}",
                                requestIndex,
                                LocalDateTime.now().format(TIME_FORMATTER),
                                endTime - requestStartTime,
                                chunkCount.get());
                        latch.countDown();
                    })
                    .doOnError(e -> {
                        log.error("[请求-{}] 发生错误: {}", requestIndex, e.getMessage());
                        latch.countDown();
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("[请求-{}] 异常: {}", requestIndex, e.getMessage(), e);
            latch.countDown();
        }
    }
}