package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户答题记录表
 * @TableName user_answer
 */
@TableName(value ="user_answer")
@Data
public class UserAnswer implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 题单ID
     */
    private Long assignmentId;

    /**
     * 题单类型
     */
    private Integer appType;

    /**
     * 评分策略
     */
    private Integer scoringStrategy;

    /**
     * 用户答案JSON
     */
    private String choices;

    /**
     * 评分结果ID
     */
    private Long resultId;

    /**
     * 
     */
    private String resultName;

    /**
     * 
     */
    private String resultDesc;

    /**
     * 
     */
    private String resultPicture;

    /**
     * 得分
     */
    private Integer resultScore;

    /**
     * 学生ID
     */
    private Long userId;

    /**
     * 是否归档
     */
    private Integer isArchive;

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
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}