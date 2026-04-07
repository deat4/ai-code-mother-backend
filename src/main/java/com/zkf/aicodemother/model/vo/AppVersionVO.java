package com.zkf.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用版本视图对象
 */
@Data
public class AppVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本 ID
     */
    private Long id;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 版本名称
     */
    private String versionName;

    /**
     * 版本摘要/变更说明
     */
    private String summary;

    /**
     * 变更类型：CREATE/UPDATE/ROLLBACK
     */
    private String changeType;

    /**
     * 与上一版本的差异摘要
     */
    private String diffSummary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建者用户 ID
     */
    private Long createdBy;

    /**
     * 创建者姓名
     */
    private String creatorName;

    /**
     * 是否为当前版本
     */
    private Boolean isCurrent;
}