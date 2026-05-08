package com.zkf.aicodemother.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
     * 任务 ID（序列化为字符串，避免前端 JS 精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 应用 ID（序列化为字符串，避免前端 JS 精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appId;

    /**
     * 用户 ID（序列化为字符串，避免前端 JS 精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
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

    /**
     * 问题数量（仅统计 ERROR）
     */
    private Integer issueCount;

    /**
     * 警告数量（仅统计 WARN）
     */
    private Integer warningCount;

    /**
     * 当前修复轮次
     */
    private Integer repairCount;

    /**
     * 最大修复轮次
     */
    private Integer maxRepairCount;

    /**
     * 修复摘要
     */
    private String repairSummary;
}