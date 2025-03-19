package com.xuesi.xuesisi.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.TeachingPlanGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeachingPlanGenerationServiceImpl implements TeachingPlanGenerationService {

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private QuestionService questionService;

    @Override
    public TeachingPlan generateTeachingPlan(QuestionBank questionBank, UserAnswerVO userAnswerVO, String aiResponse) {
        try {
            // 1. 从AI评分结果中提取错误的知识点
            List<String> weakKnowledgePoints = extractWeakKnowledgePoints(aiResponse, questionBank);
            log.info("识别出{}个薄弱知识点", weakKnowledgePoints.size());
            
            // 2. 构建教学计划生成的提示词
            String prompt = buildTeachingPlanPrompt(weakKnowledgePoints, aiResponse);
            
            // 3. 调用AI生成教学计划
            String planResponse = deepSeekService.chat(prompt);
            
            // 4. 解析AI返回的教学计划
            TeachingPlan teachingPlan = parseTeachingPlan(planResponse, questionBank, userAnswerVO);
            log.info("成功生成教学计划，用户ID: {}, 题库ID: {}", questionBank.getUserId(), questionBank.getId());
            
            return teachingPlan;
        } catch (Exception e) {
            log.error("生成教学计划失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成教学计划失败：" + e.getMessage());
        }
    }

    /**
     * 提取薄弱知识点
     */
    private List<String> extractWeakKnowledgePoints(String aiResponse, QuestionBank questionBank) {
        List<String> weakPoints = new ArrayList<>();
        try {
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI响应为空，无法解析错题知识点");
                return weakPoints;
            }

            // 移除markdown代码块标记
            aiResponse = cleanJsonResponse(aiResponse);
            
            // 检查响应是否是有效的JSON
            if (!isValidJsonObject(aiResponse)) {
                log.warn("AI响应不是有效的JSON对象格式");
                return weakPoints;
            }

            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            if (!jsonResponse.containsKey("analysis")) {
                log.warn("AI响应中缺少analysis字段");
                return weakPoints;
            }

            JSONArray analysis = jsonResponse.getJSONArray("analysis");
            if (analysis == null || analysis.isEmpty()) {
                log.warn("analysis数组为空");
                return weakPoints;
            }

            // 遍历分析结果，提取错题知识点
            for (int i = 0; i < analysis.size(); i++) {
                JSONObject questionAnalysis = analysis.getJSONObject(i);
                if (!hasRequiredFields(questionAnalysis)) {
                    continue;
                }

                int score = questionAnalysis.getInt("score", 10); // 默认10分，表示正确
                Long questionId = questionAnalysis.getLong("questionId");
                
                if (score == 0 && questionId != null) {
                    addQuestionKnowledgePoints(questionId, weakPoints);
                }
            }
        } catch (Exception e) {
            log.error("解析错题知识点失败", e);
        }
        
        // 去重并返回
        return weakPoints.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 清理JSON响应文本
     */
    private String cleanJsonResponse(String response) {
        return response.replaceAll("```json\\s*", "")
                     .replaceAll("```\\s*", "")
                     .trim();
    }
    
    /**
     * 检查字符串是否是有效的JSON对象
     */
    private boolean isValidJsonObject(String jsonStr) {
        return jsonStr.startsWith("{") && jsonStr.endsWith("}");
    }
    
    /**
     * 检查问题分析JSON是否包含必要字段
     */
    private boolean hasRequiredFields(JSONObject analysis) {
        return analysis.containsKey("score") && analysis.containsKey("questionId");
    }
    
    /**
     * 添加题目的知识点到列表
     */
    private void addQuestionKnowledgePoints(Long questionId, List<String> points) {
        Question question = questionService.getById(questionId);
        if (question != null && question.getTagsStr() != null) {
            List<String> tags = JSONUtil.parseArray(question.getTagsStr()).toList(String.class);
            if (tags != null && !tags.isEmpty()) {
                points.addAll(tags);
                log.debug("题目{}的知识点: {}", questionId, tags);
            }
        }
    }

    /**
     * 构建生成教学计划的提示词
     */
    private String buildTeachingPlanPrompt(List<String> weakPoints, String aiResponse) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下信息生成一份详细的教学教案：\n\n");
        
        // 添加薄弱知识点信息
        if (!weakPoints.isEmpty()) {
            prompt.append("1. 学生在以下知识点表现欠佳：").append(String.join("、", weakPoints)).append("\n\n");
        } else {
            prompt.append("1. 学生普遍表现良好，但需要巩固已学知识\n\n");
        }
        
        // 添加题目分析信息
        prompt.append("2. 具体题目分析：").append(aiResponse).append("\n\n");
        
        // 指定返回格式
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
        
        return prompt.toString();
    }

    /**
     * 解析AI生成的教学计划
     */
    private TeachingPlan parseTeachingPlan(String aiResponse, QuestionBank questionBank, UserAnswerVO userAnswerVO) {
        try {
            // 清理响应文本
            aiResponse = cleanJsonResponse(aiResponse);
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            
            // 创建教学计划对象
            TeachingPlan teachingPlan = new TeachingPlan();
            teachingPlan.setQuestionBankId(questionBank.getId());
            teachingPlan.setUserId(questionBank.getUserId());
            teachingPlan.setUserAnswerId(userAnswerVO.getId());
            
            // 设置知识点分析
            teachingPlan.setKnowledgeAnalysis(jsonResponse.getStr("knowledgeAnalysis"));
            
            // 设置教学设计
            JSONObject teachingDesign = jsonResponse.getJSONObject("teachingDesign");
            if (teachingDesign != null) {
                teachingPlan.setTeachingObjectives(teachingDesign.getStr("teachingObjectives"));
                JSONArray arrangement = teachingDesign.getJSONArray("teachingArrangement");
                teachingPlan.setTeachingArrangement(JSONUtil.toJsonStr(arrangement));
            }
            
            // 设置预期成果和评估方法
            teachingPlan.setExpectedOutcomes(jsonResponse.getStr("expectedOutcomes"));
            teachingPlan.setEvaluationMethods(jsonResponse.getStr("evaluationMethods"));
            
            return teachingPlan;
        } catch (Exception e) {
            log.error("解析教学计划失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解析教学计划失败：" + e.getMessage());
        }
    }
} 