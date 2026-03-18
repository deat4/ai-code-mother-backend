package com.zkf.aicodemother.core.parser;

import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;

/**
 * 代码解析执行器
 * 根据代码生成类型执行相应的解析逻辑
 *
 * @author yupi
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 执行代码解析
     *
     * @param codeContent 代码内容
     * @param codeGenType 代码生成类型
     * @return 解析结果（HtmlCodeResult 或 MultiFileCodeResult）
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }

        // 修复点：移除了重复的 switch 块，合并了 MULTI_FILE 和 VUE_PROJECT，并增加 default 兜底
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE, VUE_PROJECT -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型解析: " + codeGenType);
        };
    }
}