package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;
import com.xuesi.xuesisi.model.entity.QuestionKnowledge;

import java.util.List;

/**
 * 题目-知识点关联服务接口
 */
public interface QuestionKnowledgeService extends IService<QuestionKnowledge> {
    
    /**
     * 为题目添加知识点关联
     */
    boolean addKnowledgeToQuestion(Long questionId, Long knowledgeId);
    
    /**
     * 为题目批量添加知识点关联
     */
    boolean batchAddKnowledgeToQuestion(Long questionId, List<Long> knowledgeIds);
    
    /**
     * 删除题目的知识点关联
     */
    boolean removeKnowledgeFromQuestion(Long questionId, Long knowledgeId);
    
    /**
     * 删除题目的所有知识点关联
     */
    boolean removeAllKnowledgeFromQuestion(Long questionId);
    
    /**
     * 获取题目关联的所有知识点
     */
    List<KnowledgePoint> getKnowledgePointsByQuestionId(Long questionId);
    
    /**
     * 获取知识点关联的所有题目ID
     */
    List<Long> getQuestionIdsByKnowledgeId(Long knowledgeId);
    
    /**
     * 获取多个知识点关联的所有题目ID
     */
    List<Long> getQuestionIdsByKnowledgeIds(List<Long> knowledgeIds);
}
