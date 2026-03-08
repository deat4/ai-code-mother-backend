package com.zkf.aicodemother.core;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.zkf.aicodemother.model.vo.VersionDiffVO;

import java.util.Arrays;
import java.util.List;

/**
 * 版本差异工具类
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
public class VersionDiffUtils {

    /**
     * 计算两个文本的差异
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 差异结果
     */
    public static VersionDiffVO.DiffStats calculateDiff(String oldContent, String newContent) {
        List<String> oldLines = splitLines(oldContent);
        List<String> newLines = splitLines(newContent);

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        int additions = 0;
        int deletions = 0;
        int changes = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            DeltaType type = delta.getType();
            switch (type) {
                case INSERT:
                    additions += delta.getTarget().getLines().size();
                    break;
                case DELETE:
                    deletions += delta.getSource().getLines().size();
                    break;
                case CHANGE:
                    changes += Math.max(
                            delta.getSource().getLines().size(),
                            delta.getTarget().getLines().size()
                    );
                    break;
                default:
                    break;
            }
        }

        return new VersionDiffVO.DiffStats(
                additions,
                deletions,
                changes,
                newLines.size()
        );
    }

    /**
     * 生成差异摘要
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 差异摘要
     */
    public static String generateDiffSummary(String oldContent, String newContent) {
        VersionDiffVO.DiffStats stats = calculateDiff(oldContent, newContent);
        return String.format("+%d -%d ~%d 行",
                stats.getAdditions(),
                stats.getDeletions(),
                stats.getChanges());
    }

    /**
     * 生成带 HTML 标记的差异
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return HTML 格式的差异
     */
    public static String generateDiffHtml(String oldContent, String newContent) {
        List<String> oldLines = splitLines(oldContent);
        List<String> newLines = splitLines(newContent);

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        StringBuilder html = new StringBuilder();
        int oldPos = 0;
        int newPos = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            // 输出未变更的行
            while (oldPos < delta.getSource().getPosition()) {
                html.append("<div class=\"diff-line\">")
                        .append(escapeHtml(oldLines.get(oldPos)))
                        .append("</div>");
                oldPos++;
                newPos++;
            }

            // 输出差异
            switch (delta.getType()) {
                case DELETE:
                    for (String line : delta.getSource().getLines()) {
                        html.append("<div class=\"diff-removed\">- ")
                                .append(escapeHtml(line))
                                .append("</div>");
                    }
                    oldPos += delta.getSource().getLines().size();
                    break;
                case INSERT:
                    for (String line : delta.getTarget().getLines()) {
                        html.append("<div class=\"diff-added\">+ ")
                                .append(escapeHtml(line))
                                .append("</div>");
                    }
                    newPos += delta.getTarget().getLines().size();
                    break;
                case CHANGE:
                    for (String line : delta.getSource().getLines()) {
                        html.append("<div class=\"diff-removed\">- ")
                                .append(escapeHtml(line))
                                .append("</div>");
                    }
                    for (String line : delta.getTarget().getLines()) {
                        html.append("<div class=\"diff-added\">+ ")
                                .append(escapeHtml(line))
                                .append("</div>");
                    }
                    oldPos += delta.getSource().getLines().size();
                    newPos += delta.getTarget().getLines().size();
                    break;
                default:
                    break;
            }
        }

        // 输出剩余未变更的行
        while (oldPos < oldLines.size()) {
            html.append("<div class=\"diff-line\">")
                    .append(escapeHtml(oldLines.get(oldPos)))
                    .append("</div>");
            oldPos++;
        }

        return html.toString();
    }

    /**
     * 分割文本为行列表
     */
    private static List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return Arrays.asList("");
        }
        return Arrays.asList(content.split("\n"));
    }

    /**
     * HTML 转义
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}