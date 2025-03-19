package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.question.QuestionContentDTO;
import com.xuesi.xuesisi.model.entity.*;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.utils.AnswerMatchUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
                .orderByAsc(QuestionBankQuestion::getQuestionOrder)
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
                .orderByAsc(ScoringResult::getId)
        );
        
        if (scoringResultList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分结果不存在，请先配置评分结果");
        }

        // 3. 计算用户的总得分
        int totalScore = 0;
        List<String> detailedScores = new ArrayList<>(); // 记录每道题的得分详情
        
        for (int i = 0; i < questions.size() && i < choices.size(); i++) {
            Question question = questions.get(i);
            String userAnswer = choices.get(i);
            
            QuestionVO questionVO = QuestionVO.objToVo(question);
            if (questionVO == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目数据转换失败");
            }
            
            String content = questionVO.getContent();
            if (content == null || content.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容为空");
            }
            
            // 计算当前题目得分
            int questionScore = 0;
            String scoreDetail = "";
            
            // 根据题目类型进行不同的评分处理
            switch (questionVO.getQuestionType()) {
                case 0: // 选择题
                    // 如果答案完全匹配
                    if (questionVO.getAnswer().contains(userAnswer)) {
                        questionScore = question.getScore();
                        scoreDetail = String.format("选择题 #%d: 答案正确，得分 %d", i + 1, questionScore);
                    } else {
                        scoreDetail = String.format("选择题 #%d: 答案错误，得分 0", i + 1);
                    }
                    break;
                    
                case 1: // 填空题
                    // 获取正确答案
                    List<String> correctAnswers = questionVO.getAnswer();
                    if (correctAnswers != null && !correctAnswers.isEmpty()) {
                        double maxMatch = 0.0;
                        for (String correctAnswer : correctAnswers) {
                            double match = AnswerMatchUtils.calculateFillBlankMatch(userAnswer, correctAnswer);
                            maxMatch = Math.max(maxMatch, match);
                        }
                        questionScore = (int) Math.round(question.getScore() * maxMatch);
                        scoreDetail = String.format("填空题 #%d: 匹配度 %.2f，得分 %d", i + 1, maxMatch, questionScore);
                    }
                    break;
                    
                case 2: // 简答题
                    // 提取参考答案中的关键词
                    List<String> keywords = AnswerMatchUtils.extractKeywords(questionVO.getReferenceAnswer());
                    // 计算答案匹配度
                    double match = AnswerMatchUtils.calculateEssayMatch(
                        userAnswer,
                        questionVO.getReferenceAnswer(),
                        keywords
                    );
                    questionScore = (int) Math.round(question.getScore() * match);
                    scoreDetail = String.format("简答题 #%d: 匹配度 %.2f，得分 %d", i + 1, match, questionScore);
                    break;
                    
                default:
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的题目类型");
            }
            
            totalScore += questionScore;
            detailedScores.add(scoreDetail);
        }

        // 4. 根据得分确定最终结果
        ScoringResult finalResult = null;
        
        // 基于分数范围映射到不同等级结果
        if (totalScore >= 90) {
            // 查找"优秀"结果
            finalResult = scoringResultList.stream()
                .filter(r -> "优秀".equals(r.getResultName()))
                .findFirst()
                .orElse(null);
        } else if (totalScore >= 80) {
            // 查找"良好"结果
            finalResult = scoringResultList.stream()
                .filter(r -> "良好".equals(r.getResultName()))
                .findFirst()
                .orElse(null);
        } else if (totalScore >= 70) {
            // 查找"中等"结果
            finalResult = scoringResultList.stream()
                .filter(r -> "中等".equals(r.getResultName()))
                .findFirst()
                .orElse(null);
        } else if (totalScore >= 60) {
            // 查找"及格"结果
            finalResult = scoringResultList.stream()
                .filter(r -> "及格".equals(r.getResultName()))
                .findFirst()
                .orElse(null);
        } else {
            // 查找"不及格"结果
            finalResult = scoringResultList.stream()
                .filter(r -> "不及格".equals(r.getResultName()))
                .findFirst()
                .orElse(null);
        }
        
        // 如果没有找到匹配的结果，使用第一个结果
        if (finalResult == null && !scoringResultList.isEmpty()) {
            finalResult = scoringResultList.get(0);
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
        userAnswer.setResultDesc(String.join("\n", detailedScores)); // 添加详细得分说明
        
        return userAnswer;
    }
}

