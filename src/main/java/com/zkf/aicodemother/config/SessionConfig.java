package com.zkf.aicodemother.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.EnumSet;

/**
 * Spring Session 配置
 * 解决 SSE async dispatch 导致 session invalidated 的问题
 *
 * 通过 SseAsyncSessionFilter 在 async dispatch 时阻止 session 操作
 *
 * @author zkf
 */
@Configuration
public class SessionConfig {

    /**
     * 注册 SSE Async Session Filter
     * 在 SessionRepositoryFilter 之前执行，阻止 async dispatch 时的 session 操作
     */
    @Bean
    public SseAsyncSessionFilter sseAsyncSessionFilter() {
        return new SseAsyncSessionFilter();
    }

    /**
     * 注册 SSE Async Session Filter
     * 确保在 SessionRepositoryFilter 之前执行
     */
    @Bean
    public FilterRegistrationBean<SseAsyncSessionFilter> sseAsyncSessionFilterRegistration(
            SseAsyncSessionFilter sseAsyncSessionFilter) {

        FilterRegistrationBean<SseAsyncSessionFilter> registration =
                new FilterRegistrationBean<>(sseAsyncSessionFilter);

        // 对 REQUEST 和 ASYNC dispatcher 类型都进行过滤
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));

        // 使用最高优先级，确保在 SessionRepositoryFilter (DEFAULT_ORDER = -100) 之前执行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}