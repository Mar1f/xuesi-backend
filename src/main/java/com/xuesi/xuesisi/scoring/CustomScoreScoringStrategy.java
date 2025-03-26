package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.question.QuestionContentDTO;
import com.xuesi.xuesisi.model.dto.userAnswer.UserAnswerAddRequest;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 得分类题目评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Override
    public UserAnswer doScore(List<String> choices, QuestionBank questionBank) throws Exception {
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库信息不能为空");
        }
        
        Long questionBankId = questionBank.getId();
        
        // 1. 获取题库中的所有题目
        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
            Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
        );
        
        if (questionBankQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库中没有题目，请先添加题目");
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

        // 2. 计算用户的总得分
        int totalScore = 0;
        int questionIndex = 0;
        
        // 遍历题目进行评分
        for (Question question : questions) {
            if (questionIndex >= choices.size()) {
                break; // 如果用户答案数量少于题目数量，只计算已答题目
            }
            
            String userAnswer = choices.get(questionIndex);
            List<String> correctAnswers = question.getAnswer();
            
            // 根据题目类型进行不同的答案比较
            if (question.getQuestionType() == 1) { // 单选题
                if (correctAnswers != null && !correctAnswers.isEmpty() && 
                    userAnswer != null && correctAnswers.get(0).equals(userAnswer)) {
                    totalScore += question.getScore() != null ? question.getScore() : 10;
                }
            } else if (question.getQuestionType() == 2) { // 多选题
                // 将用户答案拆分为列表（例如："ABC" -> ["A", "B", "C"]）
                List<String> userAnswers = userAnswer != null ? 
                    userAnswer.chars()
                        .mapToObj(ch -> String.valueOf((char) ch))
                        .collect(Collectors.toList()) : 
                    new ArrayList<>();
                
                // 检查用户答案是否完全匹配正确答案
                if (correctAnswers != null && 
                    new HashSet<>(correctAnswers).equals(new HashSet<>(userAnswers))) {
                    totalScore += question.getScore() != null ? question.getScore() : 10;
                }
            }
            
            questionIndex++;
        }

        // 3. 获取题库所有评分结果
        List<ScoringResult> scoringResultList = scoringResultService.list(
            Wrappers.lambdaQuery(ScoringResult.class)
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .eq(ScoringResult::getIsDynamic, 0) // 只获取预设的结果
                .orderByAsc(ScoringResult::getId) // 按ID升序排序
        );
        
        if (scoringResultList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分结果不存在，请先配置评分结果");
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
        userAnswer.setQuestionBankType(0); // 固定为得分类型
        userAnswer.setScoringStrategy(0); // 固定为自定义评分策略
        userAnswer.setChoices(JSONUtil.toJsonStr(choices)); // 将答案列表转换为JSON字符串
        userAnswer.setResultId(finalResult.getId());
        userAnswer.setResultName(finalResult.getResultName());
        userAnswer.setResultScore(totalScore);
        userAnswer.setResultDesc(finalResult.getResultDesc()); // 添加评分描述
        userAnswer.setUserAnswerId(questionBank.getUserId()); // Set the user ID
        
        return userAnswer;
    }
}
