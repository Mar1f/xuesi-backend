package com.xuesi.xuesisi.scoring;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.event.TeachingPlanGenerationEvent;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.userAnswer.UserAnswerAddRequest;
import com.xuesi.xuesisi.model.entity.*;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.*;
import com.xuesi.xuesisi.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import cn.hutool.json.JSONConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

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
    private QuestionKnowledgeService questionKnowledgeService;
    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public UserAnswer doScore(List<String> choices, QuestionBank questionBank) throws Exception {
        validateInput(questionBank, choices);
        
        // 1. 获取题库中的所有题目及其详情（不在事务中）
        List<QuestionBankQuestion> questionBankQuestions = getQuestionBankQuestions(questionBank.getId());
        List<Question> questions = getQuestions(questionBankQuestions);
        
        // 2. 评分过程（不需要事务）
        int totalScore = 0;
        List<QuestionScoringResult> scoringResults = new ArrayList<>();
        
        for (int i = 0; i < questions.size() && i < choices.size(); i++) {
            Question question = questions.get(i);
            String userChoice = choices.get(i);
            
            QuestionScoringResult result = scoreQuestion(question, userChoice);
            scoringResults.add(result);
            totalScore += result.getScore();
            
            log.info("题目 {}: 类型={}, 得分={}, 总分={}", 
                question.getId(), 
                getQuestionTypeName(question.getQuestionType()),
                result.getScore(),
                totalScore);
        }
        
        // 3. 保存结果（使用独立的事务）
        UserAnswer userAnswer = saveResultsWithRetry(questionBank, choices, totalScore, scoringResults, questions);

        // 在保存结果后，发布事件通知教学计划生成
        eventPublisher.publishEvent(new TeachingPlanGenerationEvent(questionBank, scoringResults));

        return userAnswer;
    }
    
    /**
     * 验证输入参数
     */
    private void validateInput(QuestionBank questionBank, List<String> choices) {
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库信息不能为空");
        }
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案不能为空");
        }
    }
    
    /**
     * 获取题库中的所有题目
     */
    private List<QuestionBankQuestion> getQuestionBankQuestions(Long questionBankId) {
        List<QuestionBankQuestion> questions = questionBankQuestionService.list(
            Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .orderByAsc(QuestionBankQuestion::getQuestionOrder)
        );
        
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库中没有题目，请先添加题目");
        }
        
        return questions;
    }
    
    /**
     * 获取题目详情
     */
    private List<Question> getQuestions(List<QuestionBankQuestion> questionBankQuestions) {
        List<Long> questionIds = questionBankQuestions.stream()
            .map(QuestionBankQuestion::getQuestionId)
            .collect(Collectors.toList());
            
        List<Question> questions = questionService.listByIds(questionIds);
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目信息不存在，请检查题目数据");
        }

        return questions;
    }
    
    /**
     * 评分单个题目
     */
    private QuestionScoringResult scoreQuestion(Question question, String userChoice) {
        QuestionScoringResult result = new QuestionScoringResult();
        result.setQuestionId(question.getId());
        result.setUserAnswer(userChoice);
        result.setQuestionType(question.getQuestionType());
        
        try {
            switch (question.getQuestionType()) {
                case 0: // 单选题
                    result = scoreSingleChoice(question, userChoice);
                    break;
                case 1: // 多选题
                    result = scoreMultipleChoice(question, userChoice);
                    break;
                case 2: // 填空题
                case 3: // 简答题
                    result = scoreWithAI(question, userChoice);
                    break;
                default:
                    log.warn("未知题目类型: {}", question.getQuestionType());
                    result.setScore(0);
                    result.setAnalysis("未知题目类型");
            }
        } catch (Exception e) {
            log.error("题目评分失败: questionId={}, error={}", question.getId(), e.getMessage());
            result.setScore(0);
            result.setAnalysis("评分过程发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 评分单选题
     */
    private QuestionScoringResult scoreSingleChoice(Question question, String userChoice) {
        QuestionScoringResult result = new QuestionScoringResult();
        result.setQuestionId(question.getId());
        result.setUserAnswer(userChoice);
        
        List<String> correctAnswers = JSONUtil.parseArray(question.getAnswerStr()).toList(String.class);
        boolean isCorrect = correctAnswers != null && correctAnswers.contains(userChoice);
        
        result.setScore(isCorrect ? question.getScore() : 0);
        result.setAnalysis(isCorrect ? "答案正确" : "答案错误，正确答案：" + question.getAnswerStr());
        
        return result;
    }
    
    /**
     * 评分多选题，支持少选得部分分
     */
    private QuestionScoringResult scoreMultipleChoice(Question question, String userChoice) {
        QuestionScoringResult result = new QuestionScoringResult();
        result.setQuestionId(question.getId());
        result.setUserAnswer(userChoice);
        
        // 处理用户答案
        List<String> userAnswers;
        try {
            // 先尝试以JSON格式解析
            userAnswers = JSONUtil.parseArray(userChoice).toList(String.class);
        } catch (Exception e) {
            // 如果不是有效的JSON数组，则将其视为字符串如"ABC"
            log.debug("多选题答案不是JSON数组，尝试以字符串方式处理: {}", userChoice);
            userAnswers = new ArrayList<>();
            if (userChoice != null) {
                for (char c : userChoice.toCharArray()) {
                    if (c >= 'A' && c <= 'D') {
                        userAnswers.add(String.valueOf(c));
                    }
                }
            }
        }
        
        List<String> correctAnswers = JSONUtil.parseArray(question.getAnswerStr()).toList(String.class);
        log.debug("多选题评分 - 用户答案: {}, 正确答案: {}", userAnswers, correctAnswers);
        
        // 新的评分逻辑
        if (userAnswers == null || userAnswers.isEmpty()) {
            // 没有选择，得0分
            result.setScore(0);
            result.setAnalysis("未做答，正确答案：" + question.getAnswerStr());
            return result;
        }
        
        // 判断是否有错选（选了不在正确答案中的选项）
        boolean hasWrongSelection = false;
        for (String ua : userAnswers) {
            if (!correctAnswers.contains(ua)) {
                hasWrongSelection = true;
                break;
            }
        }
        
        if (hasWrongSelection) {
            // 多选了错误选项，得0分
            result.setScore(0);
            result.setAnalysis("选择了错误选项，正确答案：" + question.getAnswerStr());
            return result;
        }
        
        // 检查用户答案中包含了多少个正确答案
        int correctSelectionCount = 0;
        for (String ca : correctAnswers) {
            if (userAnswers.contains(ca)) {
                correctSelectionCount++;
            }
        }
        
        if (correctSelectionCount == correctAnswers.size()) {
            // 完全正确
            result.setScore(question.getScore());
            result.setAnalysis("答案正确");
        } else if (correctSelectionCount > 0) {
            // 少选（部分正确），按比例得分
            double ratio = (double) correctSelectionCount / correctAnswers.size();
            int partialScore = (int) Math.round(question.getScore() * ratio);
            result.setScore(partialScore);
            result.setAnalysis("部分正确（少选），得分：" + partialScore + "，正确答案：" + question.getAnswerStr());
        } else {
            // 完全错误
            result.setScore(0);
            result.setAnalysis("答案错误，正确答案：" + question.getAnswerStr());
        }
        
        return result;
    }
    
    /**
     * 使用AI评分（填空题和简答题）
     */
    private QuestionScoringResult scoreWithAI(Question question, String userChoice) {
        QuestionScoringResult result = new QuestionScoringResult();
        result.setQuestionId(question.getId());
        result.setUserAnswer(userChoice);
        
        try {
            // 首先尝试直接匹配答案
            List<String> correctAnswers = JSONUtil.parseArray(question.getAnswerStr()).toList(String.class);
            if (correctAnswers != null && userChoice != null) {
                String normalizedUserChoice = userChoice.trim();
                boolean isExactMatch = correctAnswers.stream()
                    .map(String::trim)
                    .anyMatch(answer -> answer.equals(normalizedUserChoice));
                
                if (isExactMatch) {
                    result.setScore(question.getScore());
                    result.setAnalysis("答案完全正确");
                    log.info("题目 {} 完全匹配，得分：{}", question.getId(), result.getScore());
                    return result;
                }
            }
            
            // 如果不是完全匹配，使用AI评分
            String prompt = question.getQuestionType() == 2 ? 
                buildFillInBlankAnalysisPrompt(question, userChoice) :
                buildEssayAnalysisPrompt(question, userChoice);
                
            String aiResponse = deepSeekService.chat(prompt);
            
            // 提取并解析JSON
            JSONObject analysisObj = extractAndParseJson(aiResponse);
            
            // 确保AI评分不超过题目分数
            int aiScore = Math.min(analysisObj.getInt("score", 0), question.getScore());
            result.setScore(aiScore);
            result.setAnalysis(analysisObj.getStr("analysis"));
            
        } catch (Exception e) {
            log.error("评分失败: questionId={}, error={}", question.getId(), e.getMessage());
            // 评分失败时给0分，不再给保底分数
            result.setScore(0);
            result.setAnalysis("评分过程发生错误，得分0分");
        }
        
        return result;
    }
    
    /**
     * 从AI响应中提取并解析JSON
     */
    private JSONObject extractAndParseJson(String response) {
        // 移除markdown代码块标记
        response = response.replaceAll("```\\s*json\\s*", "")
                         .replaceAll("```\\s*", "")
                         .trim();
        
        // 提取JSON对象
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应格式错误");
        }
        
        String jsonStr = matcher.group();
        
        // 处理可能的格式问题
        jsonStr = jsonStr.replaceAll("(?m),\\s*}", "}")  // 移除对象末尾多余的逗号
                       .replaceAll("(?m),\\s*]", "]");   // 移除数组末尾多余的逗号
        
        return JSONUtil.parseObj(jsonStr);
    }
    
    /**
     * 获取题目类型名称
     */
    private String getQuestionTypeName(Integer type) {
        switch (type) {
            case 0: return "单选题";
            case 1: return "多选题";
            case 2: return "填空题";
            case 3: return "简答题";
            default: return "未知类型";
        }
    }
    
    /**
     * 创建评分结果对象
     */
    private ScoringResult saveScoringResult(QuestionBank questionBank, int totalScore, 
                                          List<QuestionScoringResult> results) {
        ScoringResult scoringResult = new ScoringResult();
        scoringResult.setQuestionBankId(questionBank.getId());
        scoringResult.setUserId(questionBank.getUserId());
        scoringResult.setScore(totalScore);
        scoringResult.setIsDynamic(1);
        
        // 设置评分等级
        if (totalScore >= 90) {
            scoringResult.setResultName("优秀");
            scoringResult.setResultDesc("您的表现非常出色，继续保持！");
        } else if (totalScore >= 80) {
            scoringResult.setResultName("良好");
            scoringResult.setResultDesc("您的表现不错，还有提升空间。");
        } else if (totalScore >= 60) {
            scoringResult.setResultName("及格");
            scoringResult.setResultDesc("您已经达到基本要求，但需要更多练习。");
        } else {
            scoringResult.setResultName("不及格");
            scoringResult.setResultDesc("需要加强学习和练习。");
        }
        
        // 保存评分结果
        scoringResultService.save(scoringResult);
        log.info("评分结果已保存，ID: {}, 总分: {}", scoringResult.getId(), totalScore);
        
        return scoringResult;
    }
    
    /**
     * 创建用户答案对象
     */
    private UserAnswer createUserAnswer(QuestionBank questionBank, List<String> choices,
                                      ScoringResult scoringResult, int totalScore) {
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setQuestionBankId(questionBank.getId());
        userAnswer.setUserAnswerId(questionBank.getUserId());
        userAnswer.setQuestionBankType(questionBank.getQuestionBankType());
        userAnswer.setScoringStrategy(questionBank.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(scoringResult.getId());
        userAnswer.setResultName(scoringResult.getResultName());
        userAnswer.setResultDesc(scoringResult.getResultDesc());
        userAnswer.setResultScore(totalScore);
        
        return userAnswer;
    }
    
    /**
     * 保存学习分析
     */
    private void saveLearningAnalysis(QuestionBank questionBank, UserAnswer userAnswer, 
                                    List<Question> questions, List<QuestionScoringResult> results) {
        LearningAnalysis learningAnalysis = new LearningAnalysis();
        learningAnalysis.setQuestionBankId(questionBank.getId());
        
        // 获取所有题目ID
        List<Long> questionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toList());
            
        // 将题目ID列表转换为JSON字符串存储
        learningAnalysis.setQuestionId(JSONUtil.toJsonStr(questionIds));
        
        learningAnalysis.setUserId(questionBank.getUserId());
        learningAnalysis.setClassId(questionBank.getClassId());
        learningAnalysis.setTotalScore(userAnswer.getResultScore());
        learningAnalysis.setUserAnswer(userAnswer.getChoices());
        
        // 设置更详细的分析内容和建议
        StringBuilder analysisContent = new StringBuilder("## 详细题目分析\n\n");
        StringBuilder suggestionContent = new StringBuilder("## 针对性改进建议\n\n");
        
        // 计算做对和做错的题目
        int correctCount = 0;
        List<Question> incorrectQuestions = new ArrayList<>();
        Map<String, Integer> tagErrorCounts = new HashMap<>();
        
        // 第一轮：收集错题和统计
        for (int i = 0; i < results.size(); i++) {
            QuestionScoringResult result = results.get(i);
            Question question = questions.stream()
                .filter(q -> q.getId().equals(result.getQuestionId()))
                .findFirst()
                .orElse(null);
                
            if (question == null) continue;
            
            boolean isFullScore = result.getScore() >= question.getScore();
            if (isFullScore) {
                correctCount++;
            } else {
                incorrectQuestions.add(question);
                // 统计错题知识点
                List<String> tags = question.getTags();
                if (tags != null) {
                    for (String tag : tags) {
                        tagErrorCounts.put(tag, tagErrorCounts.getOrDefault(tag, 0) + 1);
                    }
                }
            }
        }
        
        // 第二轮：详细分析每道题
        for (int i = 0; i < results.size(); i++) {
            QuestionScoringResult result = results.get(i);
            Question question = questions.stream()
                .filter(q -> q.getId().equals(result.getQuestionId()))
                .findFirst()
                .orElse(null);
                
            if (question == null) continue;
            
            // 获取题型名称
            String questionTypeName = getQuestionTypeName(question.getQuestionType());
            
            // 构建题目分析
            analysisContent.append("### 题目 ").append(i + 1).append("（").append(questionTypeName).append("）\n\n");
            analysisContent.append("**题目内容**：").append(question.getQuestionContent()).append("\n\n");
            analysisContent.append("**您的答案**：").append(result.getUserAnswer()).append("\n\n");
            analysisContent.append("**正确答案**：").append(question.getAnswerStr()).append("\n\n");
            analysisContent.append("**得分**：").append(result.getScore()).append("/").append(question.getScore()).append("\n\n");
            
            // 使用AI提供的分析或生成标准分析
            if (StringUtils.isNotBlank(result.getAnalysis())) {
                analysisContent.append("**详细分析**：\n").append(result.getAnalysis()).append("\n\n");
            } else {
                boolean isCorrect = result.getScore() >= question.getScore();
                        if (isCorrect) {
                    analysisContent.append("**详细分析**：回答正确。\n\n");
                } else {
                    analysisContent.append("**详细分析**：回答有误。");
                    // 根据题型生成不同的错误分析
                    switch (question.getQuestionType()) {
                        case 0: // 单选题
                        case 1: // 多选题
                            analysisContent.append("选择的选项不正确，请仔细阅读题目要求和选项内容。\n\n");
                            break;
                        case 2: // 填空题
                            analysisContent.append("填写的答案与标准答案不符，可能是概念理解有误或表达不准确。\n\n");
                            break;
                        case 3: // 简答题
                            analysisContent.append("回答内容与期望的标准答案存在差距，可能是理解不全面或表述不充分。\n\n");
                            break;
                        default:
                            analysisContent.append("\n\n");
                    }
                }
            }
            
            // 为错题添加详细建议
            if (result.getScore() < question.getScore()) {
                suggestionContent.append("### 题目 ").append(i + 1).append(" 改进建议\n\n");
                suggestionContent.append("**题目内容**：").append(StringUtils.abbreviate(question.getQuestionContent(), 100)).append("\n\n");
                
                // 获取该题涉及的知识点
                List<String> tags = question.getTags();
                if (tags != null && !tags.isEmpty()) {
                    suggestionContent.append("**相关知识点**：").append(String.join("、", tags)).append("\n\n");
                }
                
                // 针对不同题型给出具体建议
                switch (question.getQuestionType()) {
                    case 0: // 单选题
                        suggestionContent.append("- 建议仔细阅读题目，特别关注题干中的关键词和限定条件\n");
                        suggestionContent.append("- 排除法：先排除明显错误的选项，缩小选择范围\n");
                        suggestionContent.append("- 复习以下知识点：").append(String.join("、", tags)).append("\n\n");
                        break;
                    case 1: // 多选题
                        suggestionContent.append("- 多选题需要全部选对才得分，注意不要漏选或多选\n");
                        suggestionContent.append("- 检查每个选项是否符合题目要求，不要有遗漏\n");
                        suggestionContent.append("- 复习相关概念的完整定义和范围\n\n");
                        break;
                    case 2: // 填空题
                        suggestionContent.append("- 注意专业术语的准确性和表达的规范性\n");
                        suggestionContent.append("- 理解概念的本质和区别，避免混淆相似概念\n");
                        suggestionContent.append("- 加强对").append(String.join("、", tags)).append("的基础知识学习\n\n");
                        break;
                    case 3: // 简答题
                        suggestionContent.append("- 注意回答的完整性，包含所有关键点\n");
                        suggestionContent.append("- 使用规范的专业术语表达\n");
                        suggestionContent.append("- 逻辑性：确保论述有条理、连贯\n");
                        suggestionContent.append("- 深入学习").append(String.join("、", tags)).append("相关内容\n\n");
                        break;
                    default:
                        suggestionContent.append("- 复习相关知识点，加强理解\n\n");
                }
            }
        }
        
        // 总体分析和建议
        analysisContent.append("## 总体分析\n\n");
        int totalQuestions = questions.size();
        double correctRate = totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0;
        analysisContent.append(String.format("本次测试共 %d 道题，答对 %d 题，正确率 %.1f%%。\n\n", 
                               totalQuestions, correctCount, correctRate));
        
        // 找出错误最多的知识点（最多3个）
        if (!tagErrorCounts.isEmpty()) {
            analysisContent.append("主要存在问题的知识点：");
            tagErrorCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .forEach(entry -> analysisContent.append(entry.getKey()).append("(").append(entry.getValue()).append("题)、"));
            analysisContent.delete(analysisContent.length() - 1, analysisContent.length());
            analysisContent.append("\n\n");
        }
        
        // 总体建议
        suggestionContent.append("## 整体学习建议\n\n");
        
        // 根据正确率给出不同级别的建议
        if (correctRate >= 90) {
            suggestionContent.append("您的表现非常优秀！建议：\n\n");
            suggestionContent.append("1. 挑战更高难度的题目，拓展知识深度\n");
            suggestionContent.append("2. 尝试解决一些应用性问题，提高实践能力\n");
            suggestionContent.append("3. 可以尝试辅导其他同学，通过教学加深理解\n\n");
        } else if (correctRate >= 75) {
            suggestionContent.append("您的表现良好，但仍有提升空间。建议：\n\n");
            suggestionContent.append("1. 重点复习做错的题目，特别是涉及到的知识点\n");
            suggestionContent.append("2. 系统梳理相关知识体系，找出知识盲点\n");
            suggestionContent.append("3. 做一些综合性题目，提高知识应用能力\n\n");
        } else if (correctRate >= 60) {
            suggestionContent.append("您的表现达到了基本要求，但需要加强学习。建议：\n\n");
            suggestionContent.append("1. 系统复习课程内容，尤其是错题涉及的知识点\n");
            suggestionContent.append("2. 多做针对性练习，巩固薄弱环节\n");
            suggestionContent.append("3. 建立知识框架，理清概念之间的关系\n");
            suggestionContent.append("4. 可以寻求老师或同学的帮助，解决疑难问题\n\n");
        } else {
            suggestionContent.append("您需要加强基础知识学习。建议：\n\n");
            suggestionContent.append("1. 回归基础，重新学习课程核心内容\n");
            suggestionContent.append("2. 从简单题目开始，循序渐进地提高\n");
            suggestionContent.append("3. 建立良好的学习习惯，定期复习\n");
            suggestionContent.append("4. 寻求老师的个别辅导，解决学习障碍\n");
            suggestionContent.append("5. 针对薄弱知识点制定专项学习计划\n\n");
        }
        
        // 如果有错题较多的知识点，给出针对性建议
        if (!tagErrorCounts.isEmpty()) {
            suggestionContent.append("## 重点复习知识点\n\n");
            
            List<Map.Entry<String, Integer>> sortedTags = tagErrorCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toList());
                
            for (int i = 0; i < Math.min(5, sortedTags.size()); i++) {
                Map.Entry<String, Integer> entry = sortedTags.get(i);
                suggestionContent.append("### ").append(i + 1).append(". ").append(entry.getKey()).append("\n\n");
                suggestionContent.append(String.format("该知识点在测试中出现了 %d 次错误，建议：\n\n", entry.getValue()));
                suggestionContent.append("- 回顾教材中关于此知识点的章节\n");
                suggestionContent.append("- 多做此类型的练习题\n");
                suggestionContent.append("- 理解该知识点与其他知识的联系\n\n");
            }
        }
        
        learningAnalysis.setAnalysis(analysisContent.toString());
        learningAnalysis.setSuggestion(suggestionContent.toString());
        
        // 分析知识点标签
        analyzeKnowledgeTags(questions, results, learningAnalysis);
        
        learningAnalysisService.save(learningAnalysis);
        log.info("学习分析已保存，题库ID: {}, 用户ID: {}", 
            questionBank.getId(), questionBank.getUserId());
    }

    /**
     * 分析知识点标签
     */
    private void analyzeKnowledgeTags(List<Question> questions, List<QuestionScoringResult> results, LearningAnalysis analysis) {
        // 从问题中提取标签
        Map<String, Map<String, Integer>> tagStats = new HashMap<>();
        Map<String, Map<String, Object>> knowledgePointStats = new HashMap<>();
        List<String> weakTagsAndPoints = new ArrayList<>();
        
        // 统计标签数据和知识点数据
        for (QuestionScoringResult result : results) {
            Question question = questions.stream()
                .filter(q -> q.getId().equals(result.getQuestionId()))
                .findFirst()
                .orElse(null);
                
            if (question == null) continue;
            
            boolean isCorrect = result.getScore() >= question.getScore();
            
            // 处理标签统计
            List<String> tags = question.getTags();
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    if (!tagStats.containsKey(tag)) {
                        Map<String, Integer> stats = new HashMap<>();
                        stats.put("correct", 0);
                        stats.put("total", 0);
                        tagStats.put(tag, stats);
                    }
                    
                    Map<String, Integer> stats = tagStats.get(tag);
                    stats.put("total", stats.get("total") + 1);
                    if (isCorrect) {
                        stats.put("correct", stats.get("correct") + 1);
                    }
                }
            }
            
            // 处理知识点统计
            List<KnowledgePoint> knowledgePoints = questionService.getQuestionKnowledgePoints(question.getId());
            if (knowledgePoints != null && !knowledgePoints.isEmpty()) {
                for (KnowledgePoint point : knowledgePoints) {
                    String pointId = "kp:" + point.getId(); // 添加kp:前缀区分知识点
                    
                    if (!knowledgePointStats.containsKey(pointId)) {
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("id", point.getId());
                        stats.put("name", point.getName());
                        stats.put("subject", point.getSubject());
                        stats.put("correct", 0);
                        stats.put("total", 0);
                        knowledgePointStats.put(pointId, stats);
                    }
                    
                    Map<String, Object> stats = knowledgePointStats.get(pointId);
                    stats.put("total", (Integer)stats.get("total") + 1);
                    if (isCorrect) {
                        stats.put("correct", (Integer)stats.get("correct") + 1);
                    }
                }
            }
        }
        
        // 分析薄弱标签和知识点 (低于60%正确率)
        final double CORRECT_RATE_THRESHOLD = 0.6;
        
        // 分析薄弱标签
        for (Map.Entry<String, Map<String, Integer>> entry : tagStats.entrySet()) {
            Map<String, Integer> stats = entry.getValue();
            int correctCount = stats.get("correct");
            int totalCount = stats.get("total");
            
            if (totalCount > 0 && (double) correctCount / totalCount < CORRECT_RATE_THRESHOLD) {
                weakTagsAndPoints.add(entry.getKey());
            }
        }
        
        // 分析薄弱知识点
        for (Map.Entry<String, Map<String, Object>> entry : knowledgePointStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            int correctCount = (Integer)stats.get("correct");
            int totalCount = (Integer)stats.get("total");
            
            if (totalCount > 0 && (double) correctCount / totalCount < CORRECT_RATE_THRESHOLD) {
                // 将知识点ID（带kp:前缀）添加到弱点列表
                weakTagsAndPoints.add(entry.getKey());
            }
        }
        
        // 保存统计结果
        analysis.setTagStats(JSONUtil.toJsonStr(tagStats));
        analysis.setWeakTags(JSONUtil.toJsonStr(weakTagsAndPoints));
        analysis.setKnowledgePointStats(JSONUtil.toJsonStr(knowledgePointStats));
    }

    /**
     * 保存评分结果，使用独立的事务和重试机制
     */
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    protected UserAnswer saveResultsWithRetry(QuestionBank questionBank, List<String> choices, 
                                            int totalScore, List<QuestionScoringResult> scoringResults,
                                            List<Question> questions) {
        return saveResultsInTransaction(questionBank, choices, totalScore, scoringResults, questions);
    }

    /**
     * 在事务中保存结果
     */
    @Transactional(rollbackFor = Exception.class)
    protected UserAnswer saveResultsInTransaction(QuestionBank questionBank, List<String> choices, 
                                                int totalScore, List<QuestionScoringResult> scoringResults,
                                                List<Question> questions) {
        try {
            // 保存评分结果
            ScoringResult scoringResult = saveScoringResult(questionBank, totalScore, scoringResults);
            
            // 创建并保存用户答案
            UserAnswer userAnswer = createUserAnswer(questionBank, choices, scoringResult, totalScore);
            
            // 保存学习分析
            saveLearningAnalysis(questionBank, userAnswer, questions, scoringResults);
            
            return userAnswer;
        } catch (Exception e) {
            log.error("保存评分结果失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存评分结果失败：" + e.getMessage());
        }
    }

    private String preprocessJson(String jsonStr) {
        try {
            // 移除前导和尾随空白字符
            jsonStr = jsonStr.trim();
            
            // 移除markdown代码块标记
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }
            jsonStr = jsonStr.trim();
            
            // 确保JSON以 { 开始和 } 结束
            if (!jsonStr.startsWith("{") || !jsonStr.endsWith("}")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "JSON格式不正确");
            }
            
            // 处理数学公式中的特殊字符
            jsonStr = processMathFormulas(jsonStr);
            
            // 使用Hutool的JSONUtil进行JSON验证和格式化
            try {
                // 使用更宽松的配置
                JSONConfig config = new JSONConfig();
                config.setIgnoreError(true);
                
                JSONObject jsonObject = JSONUtil.parseObj(jsonStr, config);
                
                // 验证必要字段
                if (!jsonObject.containsKey("questions")) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "JSON缺少questions字段");
                }
                JSONArray questions = jsonObject.getJSONArray("questions");
                validateQuestions(questions);
                
                // 重新序列化，确保格式正确
                return JSONUtil.toJsonStr(jsonObject);
            } catch (Exception e) {
                log.error("JSON解析失败: {}", e.getMessage());
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "JSON格式错误: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("JSON预处理失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "JSON格式错误: " + e.getMessage());
        }
    }

    /**
     * 验证题目数组的格式
     */
    private void validateQuestions(JSONArray questions) {
        if (questions == null || questions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        }
        
        for (int i = 0; i < questions.size(); i++) {
            JSONObject question = questions.getJSONObject(i);
            validateQuestion(question, i + 1);
        }
    }

    /**
     * 验证单个题目的格式
     */
    private void validateQuestion(JSONObject question, int index) {
        String[] requiredFields = {"content", "options", "answer", "analysis", "knowledgeTags"};
        for (String field : requiredFields) {
            if (!question.containsKey(field)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题缺少必要字段: %s", index, field));
            }
        }

        // 验证答案选项是否存在
        JSONArray options = question.getJSONArray("options");
        String answer = question.getStr("answer");
        Arrays.stream(answer.split(","))
                .forEach(opt -> {
                    if (!opt.matches("[A-D]")) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                String.format("第%d题答案包含非法选项: %s", index, opt));
                    }
                });
        
        // 验证知识点数组
        JSONArray tags = question.getJSONArray("knowledgeTags");
        if (tags == null || tags.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的知识点不能为空", index));
        }
    }

    /**
     * 处理数学公式中的特殊字符
     */
    private String processMathFormulas(String jsonStr) {
        // 首先处理导数符号，防止在后续处理中被过度转义
        jsonStr = jsonStr.replace("f\\'(x)", "f'(x)");
        
        Pattern pattern = Pattern.compile("\\$.*?\\$");
        Matcher matcher = pattern.matcher(jsonStr);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String formula = matcher.group()
                    .replace("{", "\\\\{")
                    .replace("}", "\\\\}")
                    .replace("_", "\\\\_")
                    .replace("^", "\\\\^");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formula));
        }
        matcher.appendTail(sb);
        
        // 修复导数表示法，将f'(x)转换为适当的转义形式
        String result = sb.toString();
        result = result.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\'(");
        
        return result;
    }

    /**
     * 构建填空题分析提示词
     */
    private String buildFillInBlankAnalysisPrompt(Question question, String userAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下填空题答案的正确性，给出0-").append(question.getScore()).append("分的评分：\n\n");
        prompt.append("题目：").append(question.getQuestionContent()).append("\n");
        prompt.append("标准答案：").append(question.getAnswerStr()).append("\n");
        prompt.append("学生答案：").append(userAnswer).append("\n\n");
        prompt.append("评分标准：\n");
        prompt.append("1. 答案的准确性（50%）：概念和术语使用是否准确\n");
        prompt.append("2. 答案的完整性（30%）：是否包含必要的关键信息\n");
        prompt.append("3. 答案的表达（20%）：是否使用了合适的专业术语\n\n");
        prompt.append("注意事项：\n");
        prompt.append("1. 如果答案完全错误，请给0分\n");
        prompt.append("2. 不要给保底分数，根据答案质量严格评分\n");
        prompt.append("3. 分数必须是整数\n\n");
        prompt.append("请返回JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 分数（0-" + question.getScore() + "）,\n");
        prompt.append("  \"analysis\": \"详细的评分分析，包括得分理由和改进建议\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 构建简答题分析提示词
     */
    private String buildEssayAnalysisPrompt(Question question, String userAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对以下简答题答案进行评分，总分").append(question.getScore()).append("分：\n\n");
        prompt.append("题目：").append(question.getQuestionContent()).append("\n");
        prompt.append("标准答案：").append(question.getAnswerStr()).append("\n");
        prompt.append("学生答案：").append(userAnswer).append("\n\n");
        prompt.append("评分标准：\n");
        prompt.append("1. 答案完整性（40%）：是否包含所有关键点\n");
        prompt.append("2. 答案准确性（30%）：概念和术语使用是否准确\n");
        prompt.append("3. 答案逻辑性（30%）：论述是否清晰合理\n\n");
        prompt.append("注意事项：\n");
        prompt.append("1. 如果答案完全错误，请给0分\n");
        prompt.append("2. 不要给保底分数，根据答案质量严格评分\n");
        prompt.append("3. 分数必须是整数\n\n");
        prompt.append("请返回JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 分数（0-" + question.getScore() + "）,\n");
        prompt.append("  \"analysis\": \"详细的评分分析，包括得分理由和改进建议\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
} 