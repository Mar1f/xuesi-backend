package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description；教学计划实体类
 * @author:mar1
 * @data:2025/03/15
 **/
@Data
@TableName(value = "teaching_plan", autoResultMap = true)
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
     * 学科
     */
    @TableField("subject")
    private String subject;

    /**
     * 知识点列表（JSON数组）
     */
    @TableField(value = "knowledge_points", typeHandler = JacksonTypeHandler.class)
    private Object knowledgePoints;

    /**
     * 知识点分析
     */
    @TableField(value = "knowledge_analysis", typeHandler = JacksonTypeHandler.class)
    private Object knowledgeAnalysis;

    /**
     * 教学目标
     */
    @TableField(value = "teaching_objectives", typeHandler = JacksonTypeHandler.class)
    private Object teachingObjectives;

    /**
     * 教学安排（JSON格式）
     */
    @TableField(value = "teaching_arrangement", typeHandler = JacksonTypeHandler.class)
    private Object teachingArrangement;

    /**
     * 预期成果
     */
    @TableField(value = "expected_outcomes", typeHandler = JacksonTypeHandler.class)
    private Object expectedOutcomes;

    /**
     * 评估方法
     */
    @TableField(value = "evaluation_methods", typeHandler = JacksonTypeHandler.class)
    private Object evaluationMethods;

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
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;
}