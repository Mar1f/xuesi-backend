package com.xuesi.xuesisi.model.dto.Learninganalysis;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class CreateLearningAnalysis {
    private Long userId;
    private Long classId;
    private Integer totalScore;
    private Double avgScore;
    /**
     * 薄弱知识点ID集合，例如：[101, 102, 105]
     */
    private List<Long> weakTags;
    /**
     * 标签统计：可以采用 JSON 字符串或其他结构，本示例使用字符串形式
     */
    private String tagStats;
}
