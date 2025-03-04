package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 学习分析统计表
 * @TableName learning_analysis
 */
@TableName(value ="learning_analysis")
@Data
public class LearningAnalysis implements Serializable {
    /**
     * 学生ID
     */
    private Long userId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 累计总分
     */
    private Integer totalScore;

    /**
     * 平均分
     */
    private BigDecimal avgScore;

    /**
     * 薄弱知识点ID集合（JSON数组）
     */
    private String weakTags;

    /**
     * 标签统计（如{"编程": {"correct": 5, "total": 10}}）
     */
    private String tagStats;

    /**
     * 
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}