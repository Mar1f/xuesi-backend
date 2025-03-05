package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.question.QuestionContentDTO;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义测评类应用评分策略
 *
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Override
    public UserAnswer doScore(List<String> choices, QuestionBank questionBank) throws Exception {
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题单信息不能为空");
        }
        
        Long questionBankId = questionBank.getId();
        
        // 1. 获取题单中的所有题目
        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
            Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
        );
        
        if (questionBankQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题单中没有题目，请先添加题目");
        }
        
        // 获取所有题目ID
        List<Long> questionIds = questionBankQuestions.stream()
            .map(QuestionBankQuestion::getQuestionId)
            .collect(Collectors.toList());
            
        // 获取题目详情
        List<Question> questions = questionService.listByIds(questionIds);
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目信息不存在，请检查题目数据");
        }
        
        // 2. 获取评分结果（按分数降序排序）
        List<ScoringResult> scoringResultList = scoringResultService.list(
            Wrappers.lambdaQuery(ScoringResult.class)
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .orderByDesc(ScoringResult::getResultScoreRange)
        );
        
        if (scoringResultList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分结果不存在，请先配置评分结果");
        }

        // 3. 计算用户的总得分
        int totalScore = 0;
        for (Question question : questions) {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            if (questionVO == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目数据转换失败");
            }
            
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
            if (questionContent == null || questionContent.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容为空");
            }
            
            // 遍历题目列表
            for (QuestionContentDTO questionContentDTO : questionContent) {
                if (questionContentDTO == null) {
                    continue;
                }
                
                List<QuestionContentDTO.Option> options = questionContentDTO.getOptions();
                if (options == null || options.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目选项为空");
                }
                
                // 遍历答案列表
                for (String answer : choices) {
                    // 遍历题目中的选项
                    for (QuestionContentDTO.Option option : options) {
                        if (option == null || option.getKey() == null) {
                            continue;
                        }
                        // 如果答案和选项的key匹配
                        if (option.getKey().equals(answer)) {
                            // 获取选项的分数
                            totalScore += option.getScore();
                        }
                    }
                }
            }
        }

        // 4. 根据得分范围确定最终结果
        ScoringResult finalResult = null;
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                finalResult = scoringResult;
                break;
            }
        }
        
        // 如果没有找到匹配的结果，使用最低档的结果
        if (finalResult == null && !scoringResultList.isEmpty()) {
            finalResult = scoringResultList.get(scoringResultList.size() - 1);
        }

        // 5. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setQuestionBankId(questionBankId);
        userAnswer.setQuestionBankType(questionBank.getQuestionBankType());
        userAnswer.setScoringStrategy(questionBank.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(finalResult.getId());
        userAnswer.setResultName(finalResult.getResultName());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}

