package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 知识点表
 * @TableName knowledge_point
 */
@TableName(value ="knowledge_point")
@Data
public class KnowledgePoint implements Serializable {
    /**
     * 知识点ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识点名称
     */
    private String name;

    /**
     * 知识点描述
     */
    private String description;

    /**
     * 学科
     */
    private String subject;

    /**
     * 创建人ID（教师或管理员）
     */
    private Long userId;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}