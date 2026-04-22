package com.zkf.aicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {

    HTML("原生 HTML 模式", "HTML"),
    MULTI_FILE("原生多文件模式", "MULTI_FILE"),  // 统一使用大写，与 core.CodeGenTypeEnum 保持一致
    VUE_PROJECT("Vue 项目模式", "vue_project");

    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举（忽略大小写）
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            // 忽略大小写匹配，处理数据库可能存储不同大小写的情况
            if (anEnum.value.equalsIgnoreCase(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
