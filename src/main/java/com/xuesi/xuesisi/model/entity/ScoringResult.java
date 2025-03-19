package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 评分结果ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 结果名称
     */
    private String resultName;

    /**
     * 结果描述
     */
    private String resultDesc;

    /**
     * 是否动态生成: 0-预设, 1-AI生成
     */
    private Integer isDynamic;

    /**
     * 题库ID
     */
    private Long questionBankId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 答题用时（秒）
     */
    private Integer duration;

    /**
     * 答题状态（0-未完成，1-已完成）
     */
    private Integer status;

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