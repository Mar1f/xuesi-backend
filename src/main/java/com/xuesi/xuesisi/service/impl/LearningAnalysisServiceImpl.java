package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.xuesi.xuesisi.service.LearningAnalysisService;
import com.xuesi.xuesisi.mapper.LearningAnalysisMapper;
import org.springframework.stereotype.Service;

/**
* @author mar1
* @description 针对表【learning_analysis(学习分析统计表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
public class LearningAnalysisServiceImpl extends ServiceImpl<LearningAnalysisMapper, LearningAnalysis> implements LearningAnalysisService {

    @Override
    public LearningAnalysis createLearningAnalysis(LearningAnalysis analysis) {
        save(analysis);
        return analysis;
    }

    @Override
    public LearningAnalysis getLearningAnalysis(Long userId, Long classId) {
        QueryWrapper<LearningAnalysis> query = new QueryWrapper<>();
        query.eq("userId", userId).eq("classId", classId);
        return getOne(query);
    }

    @Override
    public LearningAnalysis updateLearningAnalysis(Long userId, Long classId, LearningAnalysis analysis) {
        // 确保 composite key 正确设置
        analysis.setUserId(userId);
        analysis.setClassId(classId);
        boolean updated = update(analysis, new QueryWrapper<LearningAnalysis>()
                .eq("userId", userId)
                .eq("classId", classId));
        if (updated) {
            return getLearningAnalysis(userId, classId);
        }
        return null;
    }

    @Override
    public void deleteLearningAnalysis(Long userId, Long classId) {
        remove(new QueryWrapper<LearningAnalysis>()
                .eq("userId", userId)
                .eq("classId", classId));
    }

    @Override
    public Page<LearningAnalysis> getLearningAnalysisPage(int pageNumber, int pageSize) {
        Page<LearningAnalysis> page = new Page<>(pageNumber, pageSize);
        return page(page);
    }
}




