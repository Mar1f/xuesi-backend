package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题单ID
     */
    private Long questionBankId;

    /**
     * 题单类型
     */
    private Integer questionBankType;

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
     * 得分
     */
    private Integer resultScore;

    /**
     * 关联学生ID
     */
    private Long userAnswerId;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}