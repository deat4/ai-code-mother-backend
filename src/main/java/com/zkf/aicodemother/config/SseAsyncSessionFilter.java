package com.zkf.aicodemother.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * SSE Async Session Filter
 * 解决 SSE 长连接导致 Spring Session invalidated 的问题
 *
 * 原理：
 * 1. ASYNC dispatch 时：阻止 session 操作，避免尝试保存已失效的 session
 * 2. REQUEST dispatch 时：如果 session 已 invalidated，返回空的 session wrapper
 * 3. 静态资源路径（/preview/**, /static/**）：完全绕过 session 处理
 *
 * @author zkf
 */
@Slf4j
public class SseAsyncSessionFilter extends OncePerRequestFilter implements Ordered {

    /**
     * 不需要 session 的路径模式
     */
    private static final String[] NO_SESSION_PATHS = {
            "/preview/",
            "/static/"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        DispatcherType dispatcherType = request.getDispatcherType();
        String requestURI = request.getRequestURI();

        // 检查是否是不需要 session 的静态资源路径
        if (isNoSessionPath(requestURI)) {
            log.debug("No-session path detected: {}, bypassing session operations", requestURI);
            HttpServletRequest wrappedRequest = new NoSessionRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        if (dispatcherType == DispatcherType.ASYNC) {
            // ASYNC dispatch：完全阻止 session 操作
            log.debug("ASYNC dispatch detected, wrapping request to block session operations");
            HttpServletRequest wrappedRequest = new AsyncDispatchRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else if (dispatcherType == DispatcherType.REQUEST) {
            // REQUEST dispatch：检查 session 是否有效，如果无效则使用安全 wrapper
            HttpServletRequest wrappedRequest = wrapRequestIfNeeded(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // ERROR dispatch：直接放行
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 检查是否是不需要 session 的路径
     */
    private boolean isNoSessionPath(String requestURI) {
        for (String path : NO_SESSION_PATHS) {
            if (requestURI.contains(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 session 是否有效，如果有效则返回原请求，否则返回安全 wrapper
     */
    private HttpServletRequest wrapRequestIfNeeded(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                // 尝试访问 session 以检查其有效性
                session.getAttribute("test");
                // session 有效，返回原请求
                return request;
            }
        } catch (IllegalStateException e) {
            // session 已 invalidated，使用安全 wrapper
            log.debug("Session invalidated detected for request: {}", request.getRequestURI());
            return new SafeSessionRequestWrapper(request);
        }
        // 无 session，返回原请求
        return request;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    public int getOrder() {
        // 使用最高优先级，确保在任何其他 filter（包括 SessionRepositoryFilter）之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 不需要 session 的 Request Wrapper
     * 完全阻止 session 操作，用于静态资源路径
     */
    private static class NoSessionRequestWrapper extends HttpServletRequestWrapper {

        public NoSessionRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public HttpSession getSession(boolean create) {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }
    }

    /**
     * ASYNC dispatch 时的 Request Wrapper
     * 完全阻止 session 操作，避免 Spring Session 尝试保存已失效的 session
     */
    private static class AsyncDispatchRequestWrapper extends HttpServletRequestWrapper {

        public AsyncDispatchRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession() {
            return new NoOpSessionWrapper((HttpServletRequest) getRequest());
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (create) {
                return null;
            }
            return new NoOpSessionWrapper((HttpServletRequest) getRequest());
        }

        @Override
        public String getRequestedSessionId() {
            return ((HttpServletRequest) getRequest()).getRequestedSessionId();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return true;
        }
    }

    /**
     * 安全的 Request Wrapper
     * 当 session 已 invalidated 时使用，返回一个不执行任何操作的 session
     */
    private static class SafeSessionRequestWrapper extends HttpServletRequestWrapper {

        private final NoOpSessionWrapper safeSession;

        public SafeSessionRequestWrapper(HttpServletRequest request) {
            super(request);
            this.safeSession = new NoOpSessionWrapper(request);
        }

        @Override
        public HttpSession getSession() {
            return safeSession;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (create) {
                // 不创建新 session
                return null;
            }
            return safeSession;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            // 返回 false 表示 session 无效，但不抛出异常
            return false;
        }
    }

    /**
     * 不执行任何操作的 Session Wrapper
     * 所有读写操作都被忽略，避免触发 Spring Session 的 save 操作
     */
    private static class NoOpSessionWrapper implements HttpSession {

        private final HttpServletRequest originalRequest;
        private final HttpSession originalSession;

        public NoOpSessionWrapper(HttpServletRequest request) {
            this.originalRequest = request;
            HttpSession session = null;
            try {
                session = request.getSession(false);
            } catch (IllegalStateException ignored) {
                // session 已 invalidated
            }
            this.originalSession = session;
        }

        @Override
        public Object getAttribute(String name) {
            if (originalSession != null) {
                try {
                    return originalSession.getAttribute(name);
                } catch (IllegalStateException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
            // 忽略写入
        }

        @Override
        public void removeAttribute(String name) {
            // 忽略删除
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            if (originalSession != null) {
                try {
                    return originalSession.getAttributeNames();
                } catch (IllegalStateException e) {
                    return Collections.emptyEnumeration();
                }
            }
            return Collections.emptyEnumeration();
        }

        @Override
        public long getCreationTime() {
            if (originalSession != null) {
                try {
                    return originalSession.getCreationTime();
                } catch (IllegalStateException e) {
                    return 0;
                }
            }
            return 0;
        }

        @Override
        public String getId() {
            if (originalSession != null) {
                try {
                    return originalSession.getId();
                } catch (IllegalStateException e) {
                    // ignored
                }
            }
            if (originalRequest != null) {
                try {
                    return originalRequest.getRequestedSessionId();
                } catch (Exception e) {
                    // ignored
                }
            }
            return "";
        }

        @Override
        public long getLastAccessedTime() {
            if (originalSession != null) {
                try {
                    return originalSession.getLastAccessedTime();
                } catch (IllegalStateException e) {
                    return 0;
                }
            }
            return 0;
        }

        @Override
        public int getMaxInactiveInterval() {
            if (originalSession != null) {
                try {
                    return originalSession.getMaxInactiveInterval();
                } catch (IllegalStateException e) {
                    return 0;
                }
            }
            return 0;
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            // 忽略
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return originalRequest != null ? originalRequest.getServletContext() : null;
        }

        @Override
        public void invalidate() {
            // 忽略
        }

        @Override
        public boolean isNew() {
            return false;
        }
    }
}