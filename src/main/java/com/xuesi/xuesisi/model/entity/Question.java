package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题干文本
     */
    private String questionContent;

    /**
     * 标签列表（json 数组）
     */
    private String tags;

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
    private String answer;

    /**
     * 题目分值
     */
    private Integer score;

    /**
     * 来源: 0-手动, 1-AI生成
     */
    private Integer source;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}