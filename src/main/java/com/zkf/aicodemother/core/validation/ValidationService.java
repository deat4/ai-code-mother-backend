package com.zkf.aicodemother.core.validation;

/**
 * 校验服务接口
 * 统一对不同 CodeGenType 做校验分发
 */
public interface ValidationService {

    /**
     * 执行校验
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validate(ValidationContext context);

    /**
     * 校验 HTML 类型
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateHtml(ValidationContext context);

    /**
     * 校验 MULTI_FILE 类型
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateMultiFile(ValidationContext context);

    /**
     * 校验 VUE_PROJECT 类型（结构校验）
     *
     * @param context 校验上下文
     * @return 校验结果
     */
    ValidationResult validateVueProject(ValidationContext context);

    /**
     * 校验 VUE_PROJECT 构建
     *
     * @param context 校验上下文
     * @return 构建结果
     */
    BuildValidationResult validateVueBuild(ValidationContext context);
}