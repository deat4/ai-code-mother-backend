package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 代码生成场景枚举
 * 用于区分创建场景和修改场景，提供不同的工具集和系统提示词
 */
@Getter
public enum GenerationSceneEnum {

    CREATION("创建场景", "creation"),
    MODIFICATION("修改场景", "modification");

    private final String text;

    private final String value;

    GenerationSceneEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static GenerationSceneEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (GenerationSceneEnum scene : GenerationSceneEnum.values()) {
            if (scene.value.equals(value)) {
                return scene;
            }
        }
        return null;
    }
}