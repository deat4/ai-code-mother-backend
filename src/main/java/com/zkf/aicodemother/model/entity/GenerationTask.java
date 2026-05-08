package com.zkf.aicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generation_task")
public class GenerationTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用 ID
     */
    @Column("app_id")
    private Long appId;

    /**
     * 用户 ID
     */
    @Column("user_id")
    private Long userId;

    /**
     * 代码生成类型
     */
    @Column("code_gen_type")
    private String codeGenType;

    /**
     * 生成场景
     */
    @Column("scene")
    private String scene;

    /**
     * 任务状态
     */
    @Column("status")
    private String status;

    /**
     * 当前阶段
     */
    @Column("current_stage")
    private String currentStage;

    /**
     * 会话 ID
     */
    @Column("session_id")
    private String sessionId;

    /**
     * 错误信息
     */
    @Column("error_message")
    private String errorMessage;

    /**
     * 开始时间
     */
    @Column("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @Column("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 执行时长（毫秒）
     */
    @Column("duration_ms")
    private Long durationMs;

    /**
     * 校验摘要
     */
    @Column("validation_summary")
    private String validationSummary;

    /**
     * 校验是否通过
     */
    @Column("validation_passed")
    private Integer validationPassed;

    /**
     * 问题数量（仅统计 ERROR）
     */
    @Column("issue_count")
    private Integer issueCount;

    /**
     * 警告数量（仅统计 WARN）
     */
    @Column("warning_count")
    private Integer warningCount;

    /**
     * 当前修复轮次
     */
    @Column("repair_count")
    private Integer repairCount;

    /**
     * 最大修复轮次
     */
    @Column("max_repair_count")
    private Integer maxRepairCount;

    /**
     * 修复摘要
     */
    @Column("repair_summary")
    private String repairSummary;

    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("update_time")
    private LocalDateTime updateTime;
}