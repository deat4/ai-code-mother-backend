package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 任务阶段枚举
 */
@Getter
public enum GenerationTaskStageEnum {

    INIT("初始化", "init"),
    GENERATING("代码生成", "generating"),
    VALIDATING("自动校验", "validating"),
    BUILDING("项目构建", "building"),
    SCREENSHOT("截图生成", "screenshot"),
    DONE("完成", "done");

    private final String text;

    private final String value;

    GenerationTaskStageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static GenerationTaskStageEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (GenerationTaskStageEnum stage : GenerationTaskStageEnum.values()) {
            if (stage.value.equals(value)) {
                return stage;
            }
        }
        return null;
    }
}