package com.xuesi.xuesisi.model.dto.Learninganalysis;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class QueryLearningAnalysis {
    private Long userId;
    private Long classId;
    private Integer totalScore;
    private Double avgScore;
    private List<Long> weakTags;
    /**
     * 标签统计，例如 {"编程": {"correct": 5, "total": 10}}
     */
    private Map<String, TagStat> tagStats;
    private Date createTime;

    @Data
    public static class TagStat {
        private Integer correct;
        private Integer total;
    }
}