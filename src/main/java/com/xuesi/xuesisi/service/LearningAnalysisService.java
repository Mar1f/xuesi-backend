package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author mar1
* @description 针对表【learning_analysis(学习分析统计表)】的数据库操作Service
* @createDate 2025-03-02 15:47:32
*/
public interface LearningAnalysisService extends IService<LearningAnalysis> {
    LearningAnalysis createLearningAnalysis(LearningAnalysis analysis);
    LearningAnalysis getLearningAnalysis(Long userId, Long classId);
    LearningAnalysis updateLearningAnalysis(Long userId, Long classId, LearningAnalysis analysis);
    void deleteLearningAnalysis(Long userId, Long classId);
    Page<LearningAnalysis> getLearningAnalysisPage(int pageNumber, int pageSize);
}