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
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

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
    private Boolean isDelete;

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
} 