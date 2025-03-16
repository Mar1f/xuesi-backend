package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.*;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

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

    @Resource
    private TeachingPlanService teachingPlanService;
    @Resource
    private QuestionKnowledgeService questionKnowledgeService;
    @Resource
    private UserAnswerService userAnswerService;
    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
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
        
        log.info("AI 返回的评分结果: {}, 总分: {}, 最大可能分数: {}", 
            aiResponse, totalScore, maxPossibleScore);
        
        // 5. 获取评分结果（按分数降序排序）
        List<ScoringResult> scoringResultList = null;
        try {
            scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                    .eq(ScoringResult::getQuestionBankId, questionBankId)
                    .orderByDesc(ScoringResult::getResultScoreRange)
            );
            
            if (scoringResultList.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分结果不存在，请先配置评分结果");
            }
        } catch (DataAccessException e) {
            log.error("数据库访问失败，尝试重新连接: {}", e.getMessage());
            Thread.sleep(1000);
            scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                    .eq(ScoringResult::getQuestionBankId, questionBankId)
                    .orderByDesc(ScoringResult::getResultScoreRange)
            );
        }
        
        // 6. 根据得分范围确定最终结果
        ScoringResult finalResult = null;
        for (ScoringResult scoringResult : scoringResultList) {
            // 将评分结果的分数范围转换为实际分数（乘以题目数）
            int actualScoreRange = (scoringResult.getResultScoreRange() * maxPossibleScore) / 100;
            if (totalScore >= actualScoreRange) {
                finalResult = scoringResult;
                log.info("匹配到评分结果：{}，分数范围：{}，实际分数范围：{}", 
                    scoringResult.getResultName(), 
                    scoringResult.getResultScoreRange(),
                    actualScoreRange);
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
        userAnswer.setResultScore(totalScore); // 使用原始分数
        userAnswer.setResultDesc(finalResult.getResultDesc());
        userAnswer.setUserAnswerId(questionBank.getUserId()); // Set the user ID
        
        // 8. 保存学习分析结果
        saveLearningAnalysis(questions, choices, aiResponse, totalScore, questionBank);
        
        log.info("AI 评分完成，得分：{}，结果：{}", totalScore, finalResult.getResultName());
        
        // 先保存答题记录，获取ID
        userAnswerService.save(userAnswer);
        
        // 在评分完成后，生成教案
        try {
            log.info("开始生成教案，答题记录ID：{}", userAnswer.getId());
            
            // 获取错题的知识点
            List<String> wrongQuestionKnowledgePoints = getWrongQuestionKnowledgePoints(aiResponse, questions);
            log.info("获取到错题知识点：{}", wrongQuestionKnowledgePoints);

            // 构造AI提示
            String prompt = buildTeachingPlanPrompt(wrongQuestionKnowledgePoints, aiResponse);
            log.info("构造的AI提示：{}", prompt);

            // 调用AI生成教案
            log.info("开始调用AI生成教案");
            String aiTeachingPlan = deepSeekService.chat(prompt);
            log.info("AI返回的教案：{}", aiTeachingPlan);

            // 解析AI返回的教案
            TeachingPlan teachingPlan = parseTeachingPlan(aiTeachingPlan, userAnswer.getId());
            log.info("解析后的教案：{}", teachingPlan);

            // 保存教案
            boolean saved = teachingPlanService.save(teachingPlan);
            log.info("教案保存{}", saved ? "成功" : "失败");

        } catch (Exception e) {
            log.error("生成教案失败", e);
            // 这里我们不抛出异常，因为不想影响主流程
        }
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
            // 移除markdown代码块标记
            aiResponse = aiResponse.replaceAll("```json\\s*", "")
                                 .replaceAll("```\\s*", "")
                                 .trim();
            
            // 修复常见的JSON格式问题
            aiResponse = aiResponse.replaceAll("(?m)\"analysis\":\\s*\"[^\"]*\"\\s*$", "$0,") // 修复缺少逗号的行
                                 .replaceAll("(?m)\"suggestion\":\\s*\"[^\"]*\"\\s*$", "$0,") // 修复缺少逗号的行
                                 .replaceAll(",\\s*}", "}") // 移除对象末尾多余的逗号
                                 .replaceAll(",\\s*]", "]"); // 移除数组末尾多余的逗号
            
            // 尝试解析 JSON 格式
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            
            // 首先尝试从 analysis 数组中计算总分
            if (jsonResponse.containsKey("analysis")) {
                JSONArray analysisArray = jsonResponse.getJSONArray("analysis");
                int totalScore = 0;
                log.info("开始解析题目分析，共 {} 道题", analysisArray.size());
                
                for (int i = 0; i < analysisArray.size(); i++) {
                    JSONObject analysisObj = analysisArray.getJSONObject(i);
                    if (analysisObj.containsKey("score")) {
                        int questionScore = analysisObj.getInt("score");
                        totalScore += questionScore;
                        log.info("第 {} 题得分：{}", i + 1, questionScore);
                    }
                }
                
                log.info("从analysis数组计算的总分：{}", totalScore);
                if (totalScore > 0) {
                    return totalScore;
                }
            }
            
            // 如果无法从analysis计算总分，尝试使用score字段
            if (jsonResponse.containsKey("score")) {
                int score = jsonResponse.getInt("score");
                log.info("从score字段获取总分：{}", score);
                return score;
            }
            
            // 其他字段尝试
            if (jsonResponse.containsKey("totalScore")) {
                int score = jsonResponse.getInt("totalScore");
                log.info("从totalScore字段获取总分：{}", score);
                return score;
            }
            
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI响应中未找到有效的分数");
            
        } catch (Exception e) {
            log.error("JSON解析失败，原始响应: {}", aiResponse);
            log.error("错误详情: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析AI评分结果失败: " + e.getMessage());
        }
    }

    private List<String> getWrongQuestionKnowledgePoints(String aiResponse, List<Question> questions) {
        List<String> wrongPoints = new ArrayList<>();
        try {
            // 移除markdown代码块标记
            aiResponse = aiResponse.replaceAll("```json\\s*", "")
                                 .replaceAll("```\\s*", "")
                                 .trim();
                                 
            log.info("开始解析错题知识点，AI响应：{}", aiResponse);
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            JSONArray analysis = jsonResponse.getJSONArray("analysis");
            log.info("找到{}道题的分析", analysis.size());

            for (int i = 0; i < analysis.size(); i++) {
                JSONObject questionAnalysis = analysis.getJSONObject(i);
                int score = questionAnalysis.getInt("score", 10); // 默认10分，表示正确
                log.info("第{}题得分：{}", i + 1, score);
                
                if (score == 0) {
                    // 获取这道题的知识点
                    Question question = questions.get(i); // 直接从questions列表获取
                    if (question != null && question.getTags() != null) {
                        log.info("第{}题知识点：{}", i + 1, question.getTags());
                        wrongPoints.addAll(question.getTags());
                    } else {
                        log.warn("第{}题未找到知识点", i + 1);
                    }
                }
            }
            log.info("错题知识点解析完成，找到{}个知识点", wrongPoints.size());
        } catch (Exception e) {
            log.error("解析错题知识点失败", e);
        }
        return wrongPoints;
    }

    private String buildTeachingPlanPrompt(List<String> wrongPoints, String aiResponse) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下信息生成一份详细的教学教案：\n");
        prompt.append("1. 学生在以下知识点表现欠佳：").append(String.join("、", wrongPoints)).append("\n");
        prompt.append("2. 具体题目分析：").append(aiResponse).append("\n");
        prompt.append("请生成一份包含以下内容的详细教案，以JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"knowledgeAnalysis\": \"知识点分析和学生存在的问题\",\n");
        prompt.append("  \"teachingDesign\": {\n");
        prompt.append("    \"teachingObjectives\": \"教学目标\",\n");
        prompt.append("    \"teachingArrangement\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"stage\": \"教学阶段名称\",\n");
        prompt.append("        \"duration\": \"时间分配（分钟）\",\n");
        prompt.append("        \"activities\": \"具体教学活动安排\",\n");
        prompt.append("        \"methods\": \"教学方法\",\n");
        prompt.append("        \"materials\": \"教学材料和工具\"\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"expectedOutcomes\": \"预期学习成果\",\n");
        prompt.append("  \"evaluationMethods\": \"评估方法\"\n");
        prompt.append("}\n");
        prompt.append("\n请确保：\n");
        prompt.append("1. 教学设计针对性地解决学生在这些知识点上的问题\n");
        prompt.append("2. 教学活动安排合理，时间分配适当\n");
        prompt.append("3. 采用多样化的教学方法，注重学生参与\n");
        prompt.append("4. 包含具体的教学案例和练习");
        return prompt.toString();
    }

    private TeachingPlan parseTeachingPlan(String aiTeachingPlan, Long userAnswerId) {
        TeachingPlan plan = new TeachingPlan();
        try {
            // 移除markdown代码块标记
            aiTeachingPlan = aiTeachingPlan.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            JSONObject json = JSONUtil.parseObj(aiTeachingPlan);
            plan.setUserAnswerId(userAnswerId);
            
            // 解析并保存教案内容
            Object knowledgeAnalysis = json.get("knowledgeAnalysis");
            JSONObject teachingDesign = json.getJSONObject("teachingDesign");
            Object expectedOutcomes = json.get("expectedOutcomes");
            Object evaluationMethods = json.get("evaluationMethods");
            
            plan.setKnowledgeAnalysis(knowledgeAnalysis != null ? JSONUtil.toJsonStr(knowledgeAnalysis) : null);
            if (teachingDesign != null) {
                plan.setTeachingObjectives(teachingDesign.getStr("teachingObjectives"));
                plan.setTeachingArrangement(JSONUtil.toJsonStr(teachingDesign.get("teachingArrangement")));
            }
            plan.setExpectedOutcomes(expectedOutcomes != null ? JSONUtil.toJsonStr(expectedOutcomes) : null);
            plan.setEvaluationMethods(evaluationMethods != null ? JSONUtil.toJsonStr(evaluationMethods) : null);
            
            plan.setCreateTime(new Date());
            plan.setUpdateTime(new Date());
            plan.setIsDelete(0);
        } catch (Exception e) {
            log.error("解析教案失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析教案失败");
        }
        return plan;
    }
} 