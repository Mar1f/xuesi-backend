package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 学习分析 Service 接口
 */
public interface LearningAnalysisService extends IService<LearningAnalysis> {
    LearningAnalysis createLearningAnalysis(LearningAnalysis analysis);
    LearningAnalysis getLearningAnalysis(Long userId, Long classId);
    LearningAnalysis updateLearningAnalysis(Long userId, Long classId, LearningAnalysis analysis);
    void deleteLearningAnalysis(Long userId, Long classId);
    Page<LearningAnalysis> getLearningAnalysisPage(int pageNumber, int pageSize);
    
    /**
     * 根据学习分析ID生成个性化学习建议
     * @param analysisId 学习分析ID
     * @return 生成的建议文本
     */
    String generatePersonalizedSuggestions(Long analysisId);
}