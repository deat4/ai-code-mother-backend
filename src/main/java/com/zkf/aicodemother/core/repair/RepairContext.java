package com.zkf.aicodemother.core.repair;

import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 修复上下文
 * 封装一次自动修复所需的完整上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    private Long taskId;

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
     * 生成场景：CREATION / MODIFICATION
     */
    private String scene;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 当前修复轮次
     */
    private int repairRound;

    /**
     * 最大修复轮次
     */
    private int maxRepairRounds;

    /**
     * 原始校验结果（包含 issues、buildResult 等）
     */
    private ValidationResult validationResult;

    /**
     * 原始用户消息
     */
    private String originalMessage;

    /**
     * 项目根路径
     */
    private String projectRootPath;

    /**
     * 当前任务阶段（进入修复前的阶段）
     */
    private String currentStageBeforeRepair;
}