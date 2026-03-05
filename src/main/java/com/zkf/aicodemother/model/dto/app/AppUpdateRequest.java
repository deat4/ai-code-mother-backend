package com.zkf.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求（用户）
 */
@Data
public class AppUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;
}