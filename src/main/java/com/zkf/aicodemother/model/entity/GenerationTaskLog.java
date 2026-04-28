package com.zkf.aicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成任务日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generation_task_log")
public class GenerationTaskLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 任务 ID
     */
    @Column("task_id")
    private Long taskId;

    /**
     * 阶段
     */
    @Column("stage")
    private String stage;

    /**
     * 日志类型
     */
    @Column("log_type")
    private String logType;

    /**
     * 日志内容
     */
    @Column("content")
    private String content;

    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;
}