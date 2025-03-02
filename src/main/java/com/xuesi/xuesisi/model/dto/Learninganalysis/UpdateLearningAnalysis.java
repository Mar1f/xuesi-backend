package com.xuesi.xuesisi.model.dto.Learninganalysis;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class UpdateLearningAnalysis {
    private Integer totalScore;
    private Double avgScore;
    private List<Long> weakTags;
    private String tagStats;
}
