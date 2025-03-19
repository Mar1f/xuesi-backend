package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.QuestionKnowledgeMapper;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;
import com.xuesi.xuesisi.model.entity.QuestionKnowledge;
import com.xuesi.xuesisi.service.KnowledgePointService;
import com.xuesi.xuesisi.service.QuestionKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目-知识点关联服务实现类
 */
@Service
@Slf4j
public class QuestionKnowledgeServiceImpl extends ServiceImpl<QuestionKnowledgeMapper, QuestionKnowledge> implements QuestionKnowledgeService {
    
    @Resource
    private KnowledgePointService knowledgePointService;
    
    @Resource
    private QuestionKnowledgeMapper questionKnowledgeMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addKnowledgeToQuestion(Long questionId, Long knowledgeId) {
        if (questionId == null || knowledgeId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID或知识点ID不能为空");
        }
        
        // 检查关联是否已存在
        long count = this.count(Wrappers.<QuestionKnowledge>lambdaQuery()
                .eq(QuestionKnowledge::getQuestionId, questionId)
                .eq(QuestionKnowledge::getKnowledgeId, knowledgeId));
                
        if (count > 0) {
            // 关联已存在，视为成功
            return true;
        }
        
        // 创建关联
        QuestionKnowledge relation = new QuestionKnowledge();
        relation.setQuestionId(questionId);
        relation.setKnowledgeId(knowledgeId);
        relation.setCreateTime(new Date());
        
        return this.save(relation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddKnowledgeToQuestion(Long questionId, List<Long> knowledgeIds) {
        if (questionId == null || knowledgeIds == null || knowledgeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID或知识点ID列表不能为空");
        }
        
        // 获取已存在的关联
        List<Long> existingKnowledgeIds = this.list(Wrappers.<QuestionKnowledge>lambdaQuery()
                .eq(QuestionKnowledge::getQuestionId, questionId)
                .select(QuestionKnowledge::getKnowledgeId))
                .stream()
                .map(QuestionKnowledge::getKnowledgeId)
                .collect(Collectors.toList());
                
        // 过滤出需要新增的关联
        List<Long> newKnowledgeIds = knowledgeIds.stream()
                .filter(id -> !existingKnowledgeIds.contains(id))
                .collect(Collectors.toList());
                
        if (newKnowledgeIds.isEmpty()) {
            // 所有关联已存在，视为成功
            return true;
        }
        
        // 批量创建关联
        List<QuestionKnowledge> relationList = new ArrayList<>();
        Date now = new Date();
        
        for (Long knowledgeId : newKnowledgeIds) {
            QuestionKnowledge relation = new QuestionKnowledge();
            relation.setQuestionId(questionId);
            relation.setKnowledgeId(knowledgeId);
            relation.setCreateTime(now);
            relationList.add(relation);
        }
        
        return this.saveBatch(relationList);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeKnowledgeFromQuestion(Long questionId, Long knowledgeId) {
        if (questionId == null || knowledgeId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID或知识点ID不能为空");
        }
        
        return this.remove(Wrappers.<QuestionKnowledge>lambdaQuery()
                .eq(QuestionKnowledge::getQuestionId, questionId)
                .eq(QuestionKnowledge::getKnowledgeId, knowledgeId));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeAllKnowledgeFromQuestion(Long questionId) {
        if (questionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不能为空");
        }
        
        return this.remove(Wrappers.<QuestionKnowledge>lambdaQuery()
                .eq(QuestionKnowledge::getQuestionId, questionId));
    }
    
    @Override
    public List<KnowledgePoint> getKnowledgePointsByQuestionId(Long questionId) {
        if (questionId == null) {
            return Collections.emptyList();
        }
        
        // 获取知识点ID列表
        List<Long> knowledgeIds = questionKnowledgeMapper.getKnowledgeIdsByQuestionId(questionId);
        
        if (knowledgeIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 查询知识点详情
        return knowledgePointService.listByIds(knowledgeIds);
    }
    
    @Override
    public List<Long> getQuestionIdsByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) {
            return Collections.emptyList();
        }
        
        return questionKnowledgeMapper.getQuestionIdsByKnowledgeId(knowledgeId);
    }
    
    @Override
    public List<Long> getQuestionIdsByKnowledgeIds(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> questionIds = this.list(Wrappers.<QuestionKnowledge>lambdaQuery()
                .in(QuestionKnowledge::getKnowledgeId, knowledgeIds)
                .select(QuestionKnowledge::getQuestionId))
                .stream()
                .map(QuestionKnowledge::getQuestionId)
                .distinct()
                .collect(Collectors.toList());
                
        return questionIds;
    }
}




