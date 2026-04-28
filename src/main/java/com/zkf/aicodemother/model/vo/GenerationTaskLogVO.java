package com.zkf.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成任务日志 VO
 */
@Data
public class GenerationTaskLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 阶段
     */
    private String stage;

    /**
     * 日志类型
     */
    private String logType;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}