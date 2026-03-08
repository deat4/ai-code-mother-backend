package com.zkf.aicodemother.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用版本详情视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppVersionDetailVO extends AppVersionVO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 完整代码内容
     */
    private String content;

    /**
     * 创建者信息
     */
    private UserVO creator;

    /**
     * 是否可回退
     */
    private Boolean canRollback;

    /**
     * 上一个版本号
     */
    private Integer prevVersion;

    /**
     * 下一个版本号
     */
    private Integer nextVersion;
}