package com.zkf.aicodemother.model.dto.appversion;

import com.zkf.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 查询应用版本请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppVersionQueryRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 版本号（可选过滤）
     */
    private Integer versionNumber;
}