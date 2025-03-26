package com.xuesi.xuesisi.model.enums;

import cn.hutool.core.util.ObjectUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题库类型枚举
 */
public enum QuestionBankTypeEnum {

    SINGLE_CHOICE("单选题", 0),
    MULTIPLE_CHOICE("多选题", 1),
    FILL_BLANK("填空题", 2),
    SHORT_ANSWER("简答题", 3);

    private final String text;

    private final int value;

    QuestionBankTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static QuestionBankTypeEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (QuestionBankTypeEnum anEnum : QuestionBankTypeEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
