package com.xuesi.xuesisi.service;

import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionScoringResult;
import com.xuesi.xuesisi.model.entity.TeachingPlan;

import java.util.List;

/**
 * 教学计划生成服务
 */
public interface TeachingPlanGenerationService {

    /**
     * 生成教学计划
     *
     * @param questionBank 题库信息
     * @param scoringResults 评分结果
     * @param userAnswerId 用户答题记录ID
     * @return 生成的教学计划
     */
    TeachingPlan generateTeachingPlan(QuestionBank questionBank, List<QuestionScoringResult> scoringResults, Long userAnswerId);
} 