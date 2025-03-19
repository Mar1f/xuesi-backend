package com.xuesi.xuesisi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuesi.xuesisi.model.entity.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * QuestionKnowledge Mapper
 */
@Mapper
public interface QuestionKnowledgeMapper extends BaseMapper<QuestionKnowledge> {
    
    /**
     * 根据题目ID获取相关知识点ID列表
     */
    @Select("SELECT knowledgeId FROM question_knowledge WHERE questionId = #{questionId}")
    List<Long> getKnowledgeIdsByQuestionId(@Param("questionId") Long questionId);
    
    /**
     * 根据知识点ID获取相关题目ID列表
     */
    @Select("SELECT questionId FROM question_knowledge WHERE knowledgeId = #{knowledgeId}")
    List<Long> getQuestionIdsByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}




