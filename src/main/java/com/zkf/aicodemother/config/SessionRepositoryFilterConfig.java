package com.zkf.aicodemother.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.session.FindByIndexNameSessionRepository;

import java.util.EnumSet;

/**
 * Spring Session 配置
 *
 * 解决 SSE async dispatch 导致 session invalidated 的根本方案：
 * 配置 SessionRepositoryFilter 只在 REQUEST 和 ERROR dispatcher 类型下处理，
 * 禁止 ASYNC dispatcher 触发 session 操作。
 *
 * @author zkf
 */
@Configuration
public class SessionRepositoryFilterConfig {

    /**
     * 自定义注册 SessionRepositoryFilter，排除 ASYNC dispatcher
     *
     * 这会覆盖 Spring Boot 的自动配置，确保 SSE async dispatch 不触发 session save。
     */
    @Bean
    public FilterRegistrationBean<Filter> sessionRepositoryFilterRegistration(
            FindByIndexNameSessionRepository<?> sessionRepository) {

        // 创建 SessionRepositoryFilter
        SessionRepositoryFilter<?> filter = new SessionRepositoryFilter<>(sessionRepository);

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);

        // 关键配置：只在 REQUEST 和 ERROR dispatcher 类型下处理 session
        // 排除 ASYNC dispatcher，防止 SSE 完成时触发 session save 导致 invalidated 错误
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));

        // 使用 SessionRepositoryFilter 的默认 order
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);

        return registration;
    }
}