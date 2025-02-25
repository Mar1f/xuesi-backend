package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 评分结果表
 * @TableName scoring_result
 */
@TableName(value ="scoring_result")
@Data
public class ScoringResult implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 属性集合JSON
     */
    private String resultProp;

    /**
     * 得分范围（如>=80）
     */
    private Integer resultScoreRange;

    /**
     * 关联题单ID
     */
    private Long assignmentId;

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