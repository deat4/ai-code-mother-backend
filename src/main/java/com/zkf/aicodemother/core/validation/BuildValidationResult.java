package com.zkf.aicodemother.core.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Vue 构建结果
 * 单独承载构建阶段产物
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildValidationResult {

    /**
     * npm install 是否成功
     */
    private boolean installSuccess;

    /**
     * npm run build 是否成功
     */
    private boolean buildSuccess;

    /**
     * npm install 退出码
     */
    private Integer installExitCode;

    /**
     * npm run build 退出码
     */
    private Integer buildExitCode;

    /**
     * npm install 日志
     */
    private String installLog;

    /**
     * npm run build 日志
     */
    private String buildLog;

    /**
     * 关键错误列表
     */
    @Builder.Default
    private List<String> keyErrors = new ArrayList<>();

    /**
     * npm install 耗时（毫秒）
     */
    private Long installDurationMs;

    /**
     * npm run build 耗时（毫秒）
     */
    private Long buildDurationMs;

    /**
     * 是否整体成功
     */
    public boolean isOverallSuccess() {
        return installSuccess && buildSuccess;
    }

    /**
     * 添加关键错误
     */
    public void addKeyError(String error) {
        if (keyErrors == null) {
            keyErrors = new ArrayList<>();
        }
        keyErrors.add(error);
    }
}