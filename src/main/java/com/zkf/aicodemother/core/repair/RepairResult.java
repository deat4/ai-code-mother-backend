package com.zkf.aicodemother.core.repair;

import com.zkf.aicodemother.core.validation.BuildValidationResult;
import com.zkf.aicodemother.core.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 修复结果
 * 封装一次自动修复的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否执行了修复（可能因条件不满足而跳过）
     */
    private boolean attempted;

    /**
     * 修复后最终是否通过验证
     */
    private boolean success;

    /**
     * 当前修复轮次
     */
    private int repairRound;

    /**
     * 修复摘要
     */
    private String summary;

    /**
     * 修改的文件列表
     */
    @Builder.Default
    private List<String> changedFiles = new ArrayList<>();

    /**
     * 二次校验结果
     */
    private ValidationResult validationResult;

    /**
     * Vue 构建结果（仅 VUE_PROJECT）
     */
    private BuildValidationResult buildResult;

    /**
     * 跳过修复的原因
     */
    private String skippedReason;

    /**
     * 异常信息
     */
    private String errorMessage;

    /**
     * 判断是否需要继续修复
     */
    public boolean needsFurtherRepair() {
        return attempted && !success && validationResult != null
                && validationResult.getErrorCount() > 0;
    }

    /**
     * 获取修复后的 ERROR 数量
     */
    public int getErrorCountAfterRepair() {
        return validationResult != null ? validationResult.getErrorCount() : 0;
    }

    /**
     * 获取修复后的 WARN 数量
     */
    public int getWarningCountAfterRepair() {
        return validationResult != null ? validationResult.getWarningCount() : 0;
    }
}