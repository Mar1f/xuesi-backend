package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/15
 **/
@Data
@TableName("teaching_plan")
public class TeachingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 题库ID
     */
    @TableField("question_bank_id")
    private Long questionBankId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户答案ID
     */
    @TableField("user_answer_id")
    private Long userAnswerId;

    /**
     * 知识点分析
     */
    @TableField("knowledge_analysis")
    private String knowledgeAnalysis;

    /**
     * 教学目标
     */
    @TableField("teaching_objectives")
    private String teachingObjectives;

    /**
     * 教学安排（JSON格式）
     */
    @TableField("teaching_arrangement")
    private String teachingArrangement;

    /**
     * 预期成果
     */
    @TableField("expected_outcomes")
    private String expectedOutcomes;

    /**
     * 评估方法
     */
    @TableField("evaluation_methods")
    private String evaluationMethods;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}