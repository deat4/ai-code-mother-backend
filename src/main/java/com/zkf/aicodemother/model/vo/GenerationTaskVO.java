package com.zkf.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成任务 VO
 */
@Data
public class GenerationTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    private Long id;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 生成场景
     */
    private String scene;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 当前阶段
     */
    private String currentStage;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime finishedAt;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMs;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 校验摘要
     */
    private String validationSummary;

    /**
     * 校验是否通过
     */
    private Boolean validationPassed;
}