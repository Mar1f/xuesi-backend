package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 题目表
 * @TableName question
 */
@TableName(value ="question")
@Data
public class Question implements Serializable {
    /**
     * 题目ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属题单ID
     */
    private Long assignmentId;

    /**
     * 题干文本
     */
    private String questionContent;

    /**
     * 题型: 0-单选, 1-多选, 2-填空
     */
    private Integer questionType;

    /**
     * 选项（JSON数组, 如["A","B"]）
     */
    private String options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 题目分值
     */
    private Integer score;

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
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}