package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 题库ID
     */
    private Long questionBankId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 分析内容
     */
    private String analysis;

    /**
     * 改进建议
     */
    private String suggestion;

    /**
     * 是否为总体评价
     */
    private Boolean isOverall;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}