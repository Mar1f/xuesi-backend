package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/15
 **/
@Data
@TableName("teaching_plan")
public class TeachingPlan {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 答题记录ID
     */
    private Long userAnswerId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 知识点分析和学生存在的问题
     */
    private String knowledgeAnalysis;

    /**
     * 教学目标
     */
    private String teachingObjectives;

    /**
     * 教学活动安排（JSON数组，包含教学阶段、时间分配、活动安排等）
     */
    private String teachingArrangement;

    /**
     * 预期学习成果
     */
    private String expectedOutcomes;

    /**
     * 评估方法
     */
    private String evaluationMethods;

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
    @TableLogic
    private Integer isDelete;
}