package com.zkf.aicodemother.service;

import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.model.entity.GenerationTaskLog;
import com.zkf.aicodemother.model.enums.GenerationTaskStageEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskLogTypeEnum;

import java.util.List;

/**
 * 生成任务服务接口
 */
public interface GenerationTaskService {

    /**
     * 创建任务
     *
     * @param appId       应用 ID
     * @param userId      用户 ID
     * @param codeGenType 代码生成类型
     * @param scene       生成场景
     * @return 任务 ID
     */
    Long createTask(Long appId, Long userId, String codeGenType, String scene);

    /**
     * 启动任务
     *
     * @param taskId    任务 ID
     * @param sessionId 会话 ID
     */
    void startTask(Long taskId, String sessionId);

    /**
     * 更新任务阶段
     *
     * @param taskId 任务 ID
     * @param stage  新阶段
     */
    void updateStage(Long taskId, GenerationTaskStageEnum stage);

    /**
     * 标记任务成功
     *
     * @param taskId 任务 ID
     */
    void markSuccess(Long taskId);

    /**
     * 标记任务失败
     *
     * @param taskId      任务 ID
     * @param errorMessage 错误信息
     */
    void markFailed(Long taskId, String errorMessage);

    /**
     * 标记任务取消
     *
     * @param taskId 任务 ID
     */
    void markCanceled(Long taskId);

    /**
     * 添加任务日志
     *
     * @param taskId  任务 ID
     * @param stage   阶段
     * @param logType 日志类型
     * @param content 日志内容
     */
    void appendLog(Long taskId, GenerationTaskStageEnum stage, GenerationTaskLogTypeEnum logType, String content);

    /**
     * 根据 ID 获取任务
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    GenerationTask getTaskById(Long taskId);

    /**
     * 根据应用 ID 获取最新任务
     *
     * @param appId 应用 ID
     * @return 任务实体
     */
    GenerationTask getLatestTaskByAppId(Long appId);

    /**
     * 根据会话 ID 获取任务
     *
     * @param sessionId 会话 ID
     * @return 任务实体
     */
    GenerationTask getTaskBySessionId(String sessionId);

    /**
     * 获取任务日志列表
     *
     * @param taskId 任务 ID
     * @return 日志列表
     */
    List<GenerationTaskLog> getTaskLogs(Long taskId);

    /**
     * 更新校验摘要
     *
     * @param taskId    任务 ID
     * @param summary   校验摘要
     * @param passed    是否通过
     */
    void updateValidationSummary(Long taskId, String summary, boolean passed);

    /**
     * 更新校验摘要（带问题数量和警告数量）
     *
     * @param taskId      任务 ID
     * @param summary     校验摘要
     * @param passed      是否通过
     * @param issueCount  问题数量（ERROR）
     * @param warningCount 警告数量（WARN）
     */
    void updateValidationSummary(Long taskId, String summary, boolean passed, int issueCount, int warningCount);

    /**
     * 保存校验结果
     *
     * @param taskId 任务 ID
     * @param summary 校验摘要
     * @param passed 是否通过
     * @param issuesJson 问题列表 JSON
     */
    void saveValidationResult(Long taskId, String summary, boolean passed, String issuesJson);
}