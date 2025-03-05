package com.xuesi.xuesisi.scoring;

import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 评分策略执行器
 */
@Service
public class ScoringStrategyExecutor {

    @Resource
    private CustomScoreScoringStrategy customScoreScoringStrategy;
    
    @Resource
    private AIScoringStrategy aiScoringStrategy;

    /**
     * 评分
     *
     * @param choiceList 用户选择的答案列表
     * @param questionBank 题库信息
     * @return 评分结果
     * @throws Exception
     */
    public UserAnswer doScore(List<String> choiceList, QuestionBank questionBank) throws Exception {
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库信息不能为空");
        }
        
        // 根据评分策略选择不同的评分器
        Integer scoringStrategy = questionBank.getScoringStrategy();
        if (scoringStrategy == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分策略不能为空");
        }
        
        // 设置为得分类型
        questionBank.setQuestionBankType(0);
        
        // 根据评分策略选择评分器
        switch (scoringStrategy) {
            case 0: // 自定义评分
                return customScoreScoringStrategy.doScore(choiceList, questionBank);
            case 1: // AI 评分
                return aiScoringStrategy.doScore(choiceList, questionBank);
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的评分策略");
        }
    }
}
