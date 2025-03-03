package com.xuesi.xuesisi.scoring;

import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 *
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices
     * @param questionBank
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, QuestionBank questionBank) throws Exception;
}