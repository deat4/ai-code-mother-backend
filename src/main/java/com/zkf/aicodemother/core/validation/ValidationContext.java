package com.zkf.aicodemother.core.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验上下文
 * 把校验所需上下文打包，避免方法参数爆炸
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationContext {

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
     * 代码生成类型：HTML / MULTI_FILE / VUE_PROJECT
     */
    private String codeGenType;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 项目根路径
     */
    private String projectRootPath;

    /**
     * 最新版本路径
     */
    private String latestVersionPath;

    /**
     * 最新消息
     */
    private String latestMessage;

    /**
     * 生成场景：CREATION / MODIFICATION
     */
    private String scene;
}