package com.zkf.aicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 版本差异视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionDiffVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 旧版本号
     */
    private Integer oldVersion;

    /**
     * 新版本号
     */
    private Integer newVersion;

    /**
     * 旧版本内容
     */
    private String oldContent;

    /**
     * 新版本内容
     */
    private String newContent;

    /**
     * 带 HTML 标记的差异
     */
    private String diffHtml;

    /**
     * 差异统计
     */
    private DiffStats stats;

    /**
     * 差异统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 新增行数
         */
        private Integer additions;

        /**
         * 删除行数
         */
        private Integer deletions;

        /**
         * 修改行数
         */
        private Integer changes;

        /**
         * 总行数
         */
        private Integer totalLines;
    }
}