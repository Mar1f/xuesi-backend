package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 班级表
 * @TableName class
 */
@TableName(value ="class")
@Data
public class Class implements Serializable {
    /**
     * 班级ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 教师ID（关联user.id）
     */
    private Long teacherId;

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