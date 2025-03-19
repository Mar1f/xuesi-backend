package com.xuesi.xuesisi.service;

import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;

/**
 * 教学计划生成服务
 */
public interface TeachingPlanGenerationService {

    /**
     * 根据学生答题结果生成教学计划
     *
     * @param questionBank 题库信息
     * @param userAnswerVO 学生答题结果
     * @param aiResponse AI评分结果
     * @return 生成的教学计划
     */
    TeachingPlan generateTeachingPlan(QuestionBank questionBank, UserAnswerVO userAnswerVO, String aiResponse);
} 