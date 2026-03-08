package com.zkf.aicodemother.model.dto.appversion;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 版本回退请求
 */
@Data
public class AppVersionRollbackRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 目标版本号
     */
    private Integer targetVersion;
}