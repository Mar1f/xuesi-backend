package com.xuesi.xuesisi.constant;

/**
 * 评分策略枚举
 */
public enum ScoringStrategyEnum {
    MANUAL(0, "人工评分"),
    AI(1, "AI评分");

    private final int value;
    private final String text;

    ScoringStrategyEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
} 