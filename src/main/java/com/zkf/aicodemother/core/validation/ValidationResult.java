package com.zkf.aicodemother.core.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校验结果
 * 第二步最核心的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 是否整体通过
     */
    private boolean passed;

    /**
     * 一句话摘要，用于任务表、前端提示
     */
    private String summary;

    /**
     * 生成模式：HTML / MULTI_FILE / VUE_PROJECT
     */
    private String codeGenType;

    /**
     * 当前校验对应阶段：VALIDATING / BUILDING
     */
    private String stage;

    /**
     * 结构化问题列表
     */
    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Vue 工程构建结果，非 Vue 可为空
     */
    private BuildValidationResult buildResult;

    /**
     * 扩展信息：文件数、入口文件、目录摘要等
     */
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 添加问题
     */
    public void addIssue(ValidationIssue issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }

    /**
     * 获取错误数量（用于判断 passed 和 issueCount）
     */
    public int getErrorCount() {
        if (issues == null) {
            return 0;
        }
        return (int) issues.stream()
                .filter(i -> "error".equalsIgnoreCase(i.getSeverity()))
                .count();
    }

    /**
     * 获取警告数量（用于 warningCount）
     */
    public int getWarningCount() {
        if (issues == null) {
            return 0;
        }
        return (int) issues.stream()
                .filter(i -> "warn".equalsIgnoreCase(i.getSeverity()))
                .count();
    }

    /**
     * 判断是否通过（只看是否有 ERROR）
     */
    public boolean isPassedByErrors() {
        return getErrorCount() == 0;
    }
}