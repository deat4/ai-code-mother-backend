package com.zkf.aicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 变更类型枚举
 */
@Getter
public enum ChangeTypeEnum {

    CREATE("创建", "CREATE"),
    UPDATE("更新", "UPDATE"),
    ROLLBACK("回退", "ROLLBACK");

    private final String text;

    private final String value;

    ChangeTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static ChangeTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChangeTypeEnum anEnum : ChangeTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}