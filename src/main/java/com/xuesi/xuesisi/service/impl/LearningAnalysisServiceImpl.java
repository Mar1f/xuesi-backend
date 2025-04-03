package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;
import com.xuesi.xuesisi.service.LearningAnalysisService;
import com.xuesi.xuesisi.service.KnowledgePointService;
import com.xuesi.xuesisi.mapper.LearningAnalysisMapper;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.lang.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author mar1
* @description 针对表【learning_analysis(学习分析统计表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
@Slf4j
public class LearningAnalysisServiceImpl extends ServiceImpl<LearningAnalysisMapper, LearningAnalysis> implements LearningAnalysisService {

    @Resource
    private KnowledgePointService knowledgePointService;

    @Override
    public LearningAnalysis createLearningAnalysis(LearningAnalysis analysis) {
        save(analysis);
        return analysis;
    }

    @Override
    public LearningAnalysis getLearningAnalysis(Long userId, Long classId) {
        QueryWrapper<LearningAnalysis> query = new QueryWrapper<>();
        query.eq("user_id", userId)
             .eq("class_id", classId)
             .eq("isDelete", 0)
             .orderByDesc("create_time")
             .last("LIMIT 1");
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
    
    /**
     * 根据学习分析生成个性化学习建议
     * @param analysisId 学习分析ID
     * @return 生成的建议文本
     */
    public String generatePersonalizedSuggestions(Long analysisId) {
        LearningAnalysis analysis = this.getById(analysisId);
        if (analysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习分析不存在");
        }
        
        StringBuilder suggestions = new StringBuilder("# 个性化学习建议\n\n");
        
        try {
            // 解析薄弱项（标签和知识点）
            List<String> weakItems = new ArrayList<>();
            try {
                weakItems = JSONUtil.parseArray(analysis.getWeakTags()).toList(String.class);
            } catch (Exception e) {
                log.error("解析薄弱标签时出错", e);
                weakItems = Collections.emptyList();
            }
            
            // 分离普通标签和知识点ID
            List<String> weakTags = new ArrayList<>();
            List<String> weakKnowledgePointIds = new ArrayList<>();
            
            for (String item : weakItems) {
                if (item.startsWith("kp:")) {
                    // 知识点（去掉kp:前缀）
                    weakKnowledgePointIds.add(item.substring(3));
                } else {
                    // 普通标签
                    weakTags.add(item);
                }
            }
            
            // 从knowledge_point_stats获取知识点详情
            Map<String, Map<String, Object>> knowledgePointDetails = new HashMap<>();
            if (StringUtils.isNotBlank(analysis.getKnowledgePointStats())) {
                try {
                    knowledgePointDetails = JSONUtil.parseObj(analysis.getKnowledgePointStats()).toBean(
                            new TypeReference<Map<String, Map<String, Object>>>() {});
                } catch (Exception e) {
                    log.error("解析知识点统计时出错", e);
                }
            }
            
            // 如果有薄弱知识点，生成详细建议
            if (!weakKnowledgePointIds.isEmpty() && !knowledgePointDetails.isEmpty()) {
                suggestions.append("## 需要重点复习的知识点\n\n");
                
                // 按学科分组
                Map<String, List<Map<String, Object>>> pointsBySubject = new HashMap<>();
                
                // 从统计数据中提取薄弱知识点详情
                for (String weakPointId : weakKnowledgePointIds) {
                    String key = "kp:" + weakPointId;
                    if (knowledgePointDetails.containsKey(key)) {
                        Map<String, Object> pointInfo = knowledgePointDetails.get(key);
                        String subject = pointInfo.get("subject").toString();
                        
                        if (!pointsBySubject.containsKey(subject)) {
                            pointsBySubject.put(subject, new ArrayList<>());
                        }
                        pointsBySubject.get(subject).add(pointInfo);
                    }
                }
                
                // 为每个学科生成建议
                for (Map.Entry<String, List<Map<String, Object>>> entry : pointsBySubject.entrySet()) {
                    String subject = entry.getKey();
                    List<Map<String, Object>> points = entry.getValue();
                    
                    suggestions.append("### ").append(subject).append("\n\n");
                    suggestions.append("以下知识点需要加强：\n\n");
                    
                    for (Map<String, Object> point : points) {
                        suggestions.append("- **").append(point.get("name")).append("**");
                        int correct = (Integer)point.get("correct");
                        int total = (Integer)point.get("total");
                        double rate = total > 0 ? 100.0 * correct / total : 0;
                        suggestions.append(String.format(" (正确率: %.1f%%)", rate));
                        suggestions.append("\n");
                    }
                    
                    suggestions.append("\n建议学习方法：\n");
                    suggestions.append("1. 回顾教材中相关章节\n");
                    suggestions.append("2. 针对这些知识点做专项练习\n");
                    suggestions.append("3. 可以寻求老师针对这些知识点的辅导\n\n");
                }
            } else if (!weakTags.isEmpty()) {
                // 如果只有标签分析，生成基础建议
                suggestions.append("## 需要加强的知识领域\n\n");
                for (String tag : weakTags) {
                    suggestions.append("- ").append(tag).append("\n");
                }
                
                suggestions.append("\n建议：多做与上述知识领域相关的练习题，提高对这些概念的掌握程度。\n\n");
            }
            
            // 添加总体建议
            suggestions.append("## 总体学习建议\n\n");
            
            // 根据总分给出不同的建议
            int totalScore = analysis.getTotalScore() != null ? analysis.getTotalScore() : 0;
            if (totalScore >= 90) {
                suggestions.append("您的总体表现非常优秀！可以尝试更具挑战性的题目，拓展知识边界。\n");
            } else if (totalScore >= 75) {
                suggestions.append("您的基础扎实，但仍有提升空间。建议继续巩固薄弱环节，尤其是上述标记的知识点。\n");
            } else if (totalScore >= 60) {
                suggestions.append("您达到了基本要求，但需要更系统地学习。建议回顾课程内容，并针对性地练习。\n");
            } else {
                suggestions.append("建议重新学习基础知识，并寻求更多辅导。可以从简单的题目开始，逐步提高难度。\n");
            }
            
        } catch (Exception e) {
            log.error("生成个性化建议失败", e);
            suggestions.append("无法生成个性化建议，请联系管理员。");
        }
        
        return suggestions.toString();
    }
}




