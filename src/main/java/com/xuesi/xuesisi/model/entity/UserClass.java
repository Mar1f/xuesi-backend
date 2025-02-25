package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 学生班级关系表
 * @TableName user_class
 */
@TableName(value ="user_class")
@Data
public class UserClass implements Serializable {
    /**
     * 学生ID
     */
    @TableId
    private Long userId;

    /**
     * 班级ID
     */
    @TableId
    private Long classId;

    /**
     * 
     */

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}