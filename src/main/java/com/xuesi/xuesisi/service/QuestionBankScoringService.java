package com.xuesi.xuesisi.service;

import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;

import java.util.List;

/**
 * 题库评分服务接口
 */
public interface QuestionBankScoringService {

    /**
     * 初始化评分结果
     *
     * @param questionBankId 题库ID
     * @param userId 用户ID
     * @return 评分结果
     */
    ScoringResultVO initializeScoringResult(Long questionBankId, Long userId);

    /**
     * 提交答案并评分
     *
     * @param questionBankId 题库ID
     * @param userId 用户ID
     * @param answers 答案列表
     * @return 评分结果
     */
    UserAnswerVO submitAnswers(Long questionBankId, Long userId, List<String> answers);
} 