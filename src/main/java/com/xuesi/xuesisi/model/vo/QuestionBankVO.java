package com.xuesi.xuesisi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题库视图对象
 */
@Data
public class QuestionBankVO implements Serializable {
    /**
     * 题库ID
     */
    private Long id;

    /**
     * 题库名称
     */
    private String name;

    /**
     * 题库描述
     */
    private String description;

    /**
     * 题库图标
     */
    private String picture;

    /**
     * 题库类型（0-选择题，1-多选题，2-填空题，3-简答题）
     */
    private Integer questionBankType;

    /**
     * 评分策略（0-自定义，1-AI）
     */
    private Integer scoringStrategy;

    /**
     * 题目总分
     */
    private Integer totalScore;

    /**
     * 及格分
     */
    private Integer passScore;

    /**
     * 截止时间
     */
    private Date endTime;

    /**
     * 审核状态（0-待审，1-通过，2-拒绝）
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 学科
     */
    private String subject;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 平均分
     */
    private Double averageScore;

    /**
     * 最高分
     */
    private Double highestScore;

    /**
     * 最低分
     */
    private Double lowestScore;

    /**
     * 参与人数
     */
    private Integer participantCount;

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
    private Integer isDelete;
} 