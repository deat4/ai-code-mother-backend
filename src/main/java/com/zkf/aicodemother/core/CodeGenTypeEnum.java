package com.zkf.aicodemother.core;

import lombok.Getter;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {

    HTML("html", "单文件HTML代码生成"),
    MULTI_FILE("multi_file", "多文件代码生成");

    private final String value;

    private final String desc;

    CodeGenTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}