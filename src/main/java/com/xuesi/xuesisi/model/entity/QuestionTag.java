package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 题目-标签关联表
 * @TableName question_tag
 */
@TableName(value ="question_tag")
@Data
public class QuestionTag implements Serializable {
    /**
     * 
     */
    @TableId
    private Long questionId;

    /**
     * 
     */
    @TableId
    private Long tagId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}