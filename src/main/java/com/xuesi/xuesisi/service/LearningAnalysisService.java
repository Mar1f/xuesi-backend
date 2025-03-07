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
}