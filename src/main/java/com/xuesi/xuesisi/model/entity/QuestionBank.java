package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 题单表
 * @TableName question_bank
 */
@TableName(value ="question_bank")
@Data
public class QuestionBank implements Serializable {
    /**
     * 题单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题单名称
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String picture;

    /**
     * 类型: 0-得分类, 1-测评类
     */
    private Integer questionBankType;

    /**
     * 评分策略: 0-自定义, 1-AI
     */
    private Integer scoringStrategy;

    /**
     * 题单总分
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
     * 审核状态: 0-待审, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    private Integer isDelete;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 学科
     */
    private String subject;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}