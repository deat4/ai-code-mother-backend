package com.zkf.aicodemother.model.dto.appversion;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建应用版本请求
 */
@Data
public class AppVersionAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 代码内容
     */
    private String content;

    /**
     * 版本名称（可选，自动生成）
     */
    private String versionName;

    /**
     * 变更说明
     */
    private String summary;

    /**
     * 变更类型：CREATE/UPDATE/ROLLBACK
     */
    private String changeType;

    /**
     * 父版本号（回退时使用）
     */
    private Integer parentVersion;
}