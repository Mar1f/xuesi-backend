package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.service.UserAnswerService;
import com.xuesi.xuesisi.service.LearningAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 评分策略
 */
@Component
@Slf4j
@ScoringStrategyConfig(appType = 0, scoringStrategy = 1)
public class AIScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;
    
    @Resource
    private DeepSeekService deepSeekService;
    
    @Resource
    private LearningAnalysisService learningAnalysisService;

    @Override
    public UserAnswer doScore(List<String> choices, QuestionBank questionBank) throws Exception {
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库信息不能为空");
        }
        
        Long questionBankId = questionBank.getId();
        log.info("开始对题库 {} 进行AI评分", questionBankId);
        
        // 1. 获取题库中的所有题目
        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
            Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .orderByDesc(QuestionBankQuestion::getCreateTime)
                .last("LIMIT " + questionBank.getQuestionCount())
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

        // 2. 构建 AI 评分提示词
        StringBuilder aiRequest = new StringBuilder();
        aiRequest.append("你是一个专业的教师，现在需要对一组答案进行评分和分析。请严格按照以下JSON格式返回结果：\n\n");
        aiRequest.append("{\n");
        aiRequest.append("  \"score\": 总分(0-100的整数),\n");
        aiRequest.append("  \"analysis\": [\n");
        aiRequest.append("    {\n");
        aiRequest.append("      \"questionId\": 题目序号(从1开始),\n");
        aiRequest.append("      \"userAnswer\": \"学生答案\",\n");
        aiRequest.append("      \"score\": 得分(0或10分),\n");
        aiRequest.append("      \"analysis\": \"详细的分析内容\",\n");
        aiRequest.append("      \"suggestion\": \"具体的改进建议\"\n");
        aiRequest.append("    }\n");
        aiRequest.append("  ],\n");
        aiRequest.append("  \"overallAnalysis\": \"总体评价和分析\",\n");
        aiRequest.append("  \"overallSuggestion\": \"总体改进建议\"\n");
        aiRequest.append("}\n\n");
        aiRequest.append("注意事项：\n");
        aiRequest.append("1. 每道题满分为10分，只能是0分或10分\n");
        aiRequest.append("2. 分析内容要详细具体\n");
        aiRequest.append("3. 改进建议要针对性强\n");
        aiRequest.append("4. 总分是所有题目得分的总和\n\n");
        aiRequest.append("以下是题目和答案：\n\n");
        
        for (int i = 0; i < questions.size() && i < choices.size(); i++) {
            Question question = questions.get(i);
            String userAnswer = choices.get(i);
            aiRequest.append("第").append(i + 1).append("题\n");
            aiRequest.append("题目：").append(question.getQuestionContent()).append("\n");
            aiRequest.append("标准答案：").append(question.getAnswer()).append("\n");
            aiRequest.append("学生答案：").append(userAnswer).append("\n\n");
        }

        // 3. 调用 DeepSeek API 进行评分
        log.info("发送评分请求到 DeepSeek API");
        String aiResponse = deepSeekService.getAIScore(aiRequest.toString());
        log.info("收到 DeepSeek API 响应");
        
        // 4. 解析 AI 返回的分数
        int totalScore = parseScore(aiResponse);
        int maxPossibleScore = questions.size() * 10; // 计算最大可能分数
        
        log.info("AI 返回的评分结果: {}", aiResponse);
        
        // 5. 获取评分结果（按分数降序排序）
        List<ScoringResult> scoringResultList = scoringResultService.list(
            Wrappers.lambdaQuery(ScoringResult.class)
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .orderByDesc(ScoringResult::getResultScoreRange)
        );
        
        if (scoringResultList.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分结果不存在，请先配置评分结果");
        }

        // 6. 根据得分范围确定最终结果
        ScoringResult finalResult = null;
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                finalResult = scoringResult;
                log.info("匹配到评分结果：{}", scoringResult.getResultName());
                break;
            }
        }
        
        // 如果没有找到匹配的结果，使用最低档的结果
        if (finalResult == null && !scoringResultList.isEmpty()) {
            finalResult = scoringResultList.get(scoringResultList.size() - 1);
            log.info("使用最低档评分结果：{}", finalResult.getResultName());
        }

        // 7. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setQuestionBankId(questionBankId);
        userAnswer.setQuestionBankType(0); // 得分类型
        userAnswer.setScoringStrategy(1); // AI 评分策略
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(finalResult.getId());
        userAnswer.setResultName(finalResult.getResultName());
        userAnswer.setResultScore(totalScore);
        // 保存 AI 的详细分析结果
        userAnswer.setResultDesc(aiResponse);
        
        // 8. 保存学习分析结果
        saveLearningAnalysis(questions, choices, aiResponse, totalScore, questionBank);
        
        log.info("AI 评分完成，得分：{}，结果：{}", totalScore, finalResult.getResultName());
        return userAnswer;
    }

    /**
     * 保存学习分析结果
     */
    private void saveLearningAnalysis(List<Question> questions, List<String> choices, String aiResponse, int totalScore, QuestionBank questionBank) {
        try {
            // 移除 JSON 代码块标记
            aiResponse = aiResponse.replaceAll("```json\\s*", "").replaceAll("\\s*```", "");
            
            // 解析 JSON 响应
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            JSONArray analysisArray = jsonResponse.getJSONArray("analysis");
            String overallAnalysis = jsonResponse.getStr("overallAnalysis");
            String overallSuggestion = jsonResponse.getStr("overallSuggestion");
            
            if (analysisArray == null || analysisArray.isEmpty()) {
                log.warn("未找到题目分析数据");
                return;
            }
            
            // 保存每道题的分析
            for (int i = 0; i < analysisArray.size(); i++) {
                JSONObject analysisObj = analysisArray.getJSONObject(i);
                Question question = questions.get(i);
                
                LearningAnalysis learningAnalysis = new LearningAnalysis();
                learningAnalysis.setQuestionBankId(questionBank.getId());
                learningAnalysis.setQuestionId(question.getId());
                learningAnalysis.setUserAnswer(analysisObj.getStr("userAnswer"));
                learningAnalysis.setScore(analysisObj.getInt("score"));
                learningAnalysis.setAnalysis(analysisObj.getStr("analysis"));
                learningAnalysis.setSuggestion(analysisObj.getStr("suggestion"));
                learningAnalysis.setIsOverall(false);
                
                learningAnalysisService.save(learningAnalysis);
                log.info("保存题目分析成功，题目ID：{}", question.getId());
            }
            
            // 保存总体评价
            LearningAnalysis overall = new LearningAnalysis();
            overall.setQuestionBankId(questionBank.getId());
            overall.setScore(totalScore);
            overall.setAnalysis(overallAnalysis);
            overall.setSuggestion(overallSuggestion);
            overall.setIsOverall(true);
            
            learningAnalysisService.save(overall);
            
            log.info("成功保存学习分析结果，题库ID：{}", questionBank.getId());
        } catch (Exception e) {
            log.error("保存学习分析结果失败: {}", e.getMessage(), e);
        }
    }
    
    private String extractSuggestionFromAnalysis(String analysis) {
        // 从分析文本中提取建议
        // 通常是分析文本的后半部分，从"建议"或"因此"开始
        String[] parts = analysis.split("因此|建议");
        return parts.length > 1 ? parts[1].trim() : analysis;
    }

    private int parseScore(String aiResponse) {
        log.info("AI 评分结果原文: {}", aiResponse);
        
        try {
            // 尝试解析 JSON 格式
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            if (jsonResponse.containsKey("totalScore")) {
                return jsonResponse.getInt("totalScore");
            }
            if (jsonResponse.containsKey("score")) {
                return jsonResponse.getInt("score");
            }
            if (jsonResponse.containsKey("total")) {
                return jsonResponse.getInt("total");
            }
            if (jsonResponse.containsKey("questions")) {
                JSONArray questions = jsonResponse.getJSONArray("questions");
                int totalScore = 0;
                for (int i = 0; i < questions.size(); i++) {
                    JSONObject question = questions.getJSONObject(i);
                    if (question.containsKey("score")) {
                        totalScore += question.getInt("score");
                    }
                }
                return totalScore;
            }
        } catch (Exception e) {
            log.warn("JSON 解析失败，尝试解析文本格式: {}", e.getMessage());
        }

        // 尝试解析文本格式
        try {
            // 匹配总分格式：总分：24/40 或 总分：24 或 得分：24分
            Pattern[] totalScorePatterns = {
                Pattern.compile("总分[：:](\\s*)(\\d+)/(\\d+)"),  // 匹配 总分：24/40
                Pattern.compile("总分[：:](\\s*)(\\d+)分?"),      // 匹配 总分：24 或 总分：24分
                Pattern.compile("得分[：:](\\s*)(\\d+)分?"),      // 匹配 得分：24 或 得分：24分
                Pattern.compile("(\\d+)分"),                      // 匹配 24分
                Pattern.compile("(\\d+)/(\\d+)")                  // 匹配 24/40
            };

            for (Pattern pattern : totalScorePatterns) {
                Matcher matcher = pattern.matcher(aiResponse);
                if (matcher.find()) {
                    if (matcher.groupCount() >= 3) {
                        // 格式：总分：24/40
                        int score = Integer.parseInt(matcher.group(2));
                        int total = Integer.parseInt(matcher.group(3));
                        return (int) ((score * 100.0) / total);
                    } else {
                        // 格式：总分：24 或 得分：24分 或 24分
                        int score = Integer.parseInt(matcher.group(matcher.groupCount()));
                        return score;
                    }
                }
            }

            // 匹配单个题目分数：第1题：9/10分
            Pattern questionScorePattern = Pattern.compile("第\\d+题[：:](\\s*)(\\d+)/(\\d+)分");
            Matcher matcher = questionScorePattern.matcher(aiResponse);
            int totalScore = 0;
            int totalPossible = 0;
            while (matcher.find()) {
                int score = Integer.parseInt(matcher.group(2));
                int possible = Integer.parseInt(matcher.group(3));
                totalScore += score;
                totalPossible += possible;
            }
            if (totalPossible > 0) {
                return (int) ((totalScore * 100.0) / totalPossible);
            }

            // 如果以上都没匹配到，尝试提取文本中的数字
            Pattern numberPattern = Pattern.compile("\\d+");
            matcher = numberPattern.matcher(aiResponse);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        } catch (Exception e) {
            log.error("文本格式解析失败: {}", e.getMessage());
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法解析 AI 评分结果");
    }
} 