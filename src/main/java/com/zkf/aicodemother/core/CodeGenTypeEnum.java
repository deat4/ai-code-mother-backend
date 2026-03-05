package com.zkf.aicodemother.core;

import lombok.Getter;

import java.util.Arrays;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {

    HTML("HTML", "单文件HTML代码生成"),
    MULTI_FILE("MULTI_FILE", "多文件代码生成");

    private final String value;

    private final String desc;

    CodeGenTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据 value 获取枚举（忽略大小写）
     *
     * @param value 枚举值
     * @return 枚举对象，未找到返回 null
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        // 忽略大小写匹配
        return Arrays.stream(CodeGenTypeEnum.values())
                .filter(e -> e.getValue().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}