package com.zkf.aicodemother.service;

import com.zkf.aicodemother.core.validation.ValidationContext;
import com.zkf.aicodemother.core.validation.ValidationResult;

/**
 * 生成校验编排器
 * 在生成成功后触发校验、更新任务阶段、写任务日志、推 SSE 事件、根据结果决定成功或失败
 */
public interface GenerationValidationOrchestrator {

    /**
     * 校验并更新任务状态
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateAndUpdateTask(ValidationContext context);

    /**
     * 校验 HTML/MULTI_FILE 类型
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateHtmlOrMultiFile(ValidationContext context);

    /**
     * 校验 VUE_PROJECT 类型（结构校验 + 构建）
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateVueProject(ValidationContext context);
}