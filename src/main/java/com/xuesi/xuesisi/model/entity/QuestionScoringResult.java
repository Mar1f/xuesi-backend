package com.xuesi.xuesisi.model.entity;

import lombok.Data;

/**
 * 题目评分结果
 */
@Data
public class QuestionScoringResult {
    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目类型
     */
    private Integer questionType;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 分析说明
     */
    private String analysis;
} 