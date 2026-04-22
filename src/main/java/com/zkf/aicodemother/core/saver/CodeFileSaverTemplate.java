package com.zkf.aicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.core.CodeGenTypeEnum;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存器 - 模板方法模式
 *
 * @author yupi
 */
public abstract class CodeFileSaverTemplate<T> {

    /**
     * 文件保存根目录
     */
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir();
        // 3. 保存文件（具体实现由子类提供）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 模板方法：保存代码的标准流程（使用 appId）
     *
     * @param result 代码结果对象
     * @param appId  应用 ID
     * @return 保存的目录
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建基于 appId 的目录
        String baseDirPath = buildUniqueDir(appId);
        // 3. 保存文件（具体实现由子类提供）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路径
     *
     * @return 目录路径
     */
    protected final String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 构建基于 appId 的目录路径
     *
     * @param appId 应用 ID
     * @return 目录路径
     */
    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.del(dirPath);
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件的工具方法
     * 在写入前做兜底清洗，去除可能残留的 markdown 标记
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            // 兜底清洗：去除 markdown 标记
            content = sanitizeBeforeWrite(filename, content);
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 写入文件前的兜底清洗
     * 确保即使解析器漏处理，markdown 标记也不会被写入文件
     * 同时修正 HTML 资源路径，确保与当前部署器结构一致
     */
    private String sanitizeBeforeWrite(String filename, String content) {
        String trimmed = content.trim();

        // 根据文件类型去除对应的语言标记
        if (filename.endsWith(".html")) {
            trimmed = stripFence(trimmed, "html");
            // 修正 HTML 资源路径，确保与当前部署器结构一致
            trimmed = normalizeHtmlAssetPaths(trimmed);
        } else if (filename.endsWith(".css")) {
            trimmed = stripFence(trimmed, "css");
        } else if (filename.endsWith(".js")) {
            trimmed = stripFence(trimmed, "javascript");
            trimmed = stripFence(trimmed, "js");
        }

        // 兜底去除无语言标记的 ```
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak > 0) {
                trimmed = trimmed.substring(firstLineBreak + 1).trim();
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        return trimmed;
    }

    /**
     * 修正 HTML 资源路径
     * 当前部署器只支持根目录的 style.css 和 script.js
     * 将所有 css/xxx.css 引用改为 style.css
     * 将所有 js/xxx.js 引用改为 script.js
     */
    private String normalizeHtmlAssetPaths(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // 修正 CSS 引用：css/style.css, css/responsive.css 等 -> style.css
        html = html.replaceAll("href\\s*=\\s*\"(?:\\./)?css/[^\"/]+\\.css\"", "href=\"style.css\"");

        // 修正 JS 引用：js/app.js, js/data.js 等 -> script.js
        html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?js/[^\"/]+\\.js\"", "src=\"script.js\"");

        // 同时修正 styles.css -> style.css（常见变体）
        html = html.replaceAll("href\\s*=\\s*\"(?:\\./)?styles\\.css\"", "href=\"style.css\"");

        // 修正 app.js, main.js -> script.js（常见变体）
        html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?app\\.js\"", "src=\"script.js\"");
        html = html.replaceAll("src\\s*=\\s*\"(?:\\./)?main\\.js\"", "src=\"script.js\"");

        return html;
    }

    /**
     * 去除特定语言的 markdown fence 标记
     */
    private String stripFence(String content, String lang) {
        String prefix = "```" + lang;
        if (content.startsWith(prefix)) {
            content = content.substring(prefix.length()).trim();
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3).trim();
        }
        return content;
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}