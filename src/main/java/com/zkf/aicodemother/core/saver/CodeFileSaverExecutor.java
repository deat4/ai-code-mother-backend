package com.zkf.aicodemother.core.saver;

import com.zkf.aicodemother.ai.model.HtmlCodeResult;
import com.zkf.aicodemother.ai.model.MultiFileCodeResult;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 *
 * @author yupi
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行代码保存
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }

        // 修复点：删除了下方重复残留的 switch 块，并补充 default
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult);
            case MULTI_FILE, VUE_PROJECT -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型保存: " + codeGenType);
        };
    }

    /**
     * 执行代码保存（使用 appId）
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }

        // 修复点：删除了下方重复残留的 switch 块，并补充 default
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE, VUE_PROJECT -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型保存: " + codeGenType);
        };
    }
}