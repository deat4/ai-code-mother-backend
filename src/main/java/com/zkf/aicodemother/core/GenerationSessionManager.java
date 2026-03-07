package com.zkf.aicodemother.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 生成会话管理器
 * 用于追踪和管理活跃的生成任务，支持中断操作
 */
@Slf4j
@Component
public class GenerationSessionManager {

    /**
     * 存储活跃的生成会话
     * key: sessionId, value: Disposable (可取消的订阅)
     */
    private final Map<String, Disposable> activeSessions = new ConcurrentHashMap<>();

    /**
     * 创建新的生成会话
     *
     * @return 会话ID
     */
    public String createSession() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 注册一个活跃的生成会话
     *
     * @param sessionId  会话ID
     * @param disposable 可取消的订阅
     */
    public void registerSession(String sessionId, Disposable disposable) {
        if (sessionId != null && disposable != null) {
            activeSessions.put(sessionId, disposable);
            log.info("注册生成会话: {}", sessionId);
        }
    }

    /**
     * 取消并移除一个生成会话
     *
     * @param sessionId 会话ID
     * @return 是否成功取消
     */
    public boolean cancelSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Disposable disposable = activeSessions.remove(sessionId);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            log.info("已取消生成会话: {}", sessionId);
            return true;
        }
        return false;
    }

    /**
     * 移除会话（不取消，用于正常结束时清理）
     *
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
            log.debug("移除生成会话: {}", sessionId);
        }
    }

    /**
     * 检查会话是否存在且活跃
     *
     * @param sessionId 会话ID
     * @return 是否活跃
     */
    public boolean isSessionActive(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Disposable disposable = activeSessions.get(sessionId);
        return disposable != null && !disposable.isDisposed();
    }

    /**
     * 获取活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}