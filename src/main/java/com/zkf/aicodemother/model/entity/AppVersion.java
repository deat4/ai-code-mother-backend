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
 * 应用版本实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version")
public class AppVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本 ID
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用 ID
     */
    @Column("app_id")
    private Long appId;

    /**
     * 版本号 (从 1 开始)
     */
    @Column("version_number")
    private Integer versionNumber;

    /**
     * 版本名称
     */
    @Column("version_name")
    private String versionName;

    /**
     * 完整代码内容
     */
    private String content;

    /**
     * 版本摘要/变更说明
     */
    private String summary;

    /**
     * 变更类型：CREATE/UPDATE/ROLLBACK
     */
    @Column("change_type")
    private String changeType;

    /**
     * 与上一版本的差异摘要
     */
    @Column("diff_summary")
    private String diffSummary;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 创建者用户 ID
     */
    @Column("created_by")
    private Long createdBy;

    /**
     * 是否为当前版本
     */
    @Column("is_current")
    private Integer isCurrent;

    /**
     * 父版本号
     */
    @Column("parent_version")
    private Integer parentVersion;
}