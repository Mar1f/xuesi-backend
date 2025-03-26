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
        
        // 从分析中提取学科信息
        String subject = extractSubjectFromAnalysis(aiResponse);
        
        // 添加学科特定的指导
        if (subject != null) {
            prompt.append("3. 本次教学针对的是").append(subject).append("学科，请确保教案内容与").append(subject).append("学科的教学特点和方法相符合。\n\n");
            
            // 为不同学科添加特定的教学建议
            if (subject.contains("数学")) {
                prompt.append("数学教学建议：\n");
                prompt.append("- 注重概念理解和公式推导过程\n");
                prompt.append("- 强调逻辑思维和解题策略\n");
                prompt.append("- 设计由浅入深的例题和练习\n");
                prompt.append("- 鼓励学生进行数学思维的表达与交流\n\n");
            } else if (subject.contains("语文")) {
                prompt.append("语文教学建议：\n");
                prompt.append("- 注重语言表达和文学欣赏能力的培养\n");
                prompt.append("- 结合阅读理解和写作训练\n");
                prompt.append("- 鼓励学生进行文本分析和思想交流\n");
                prompt.append("- 培养学生的人文素养和批判性思维\n\n");
            } else if (subject.contains("英语")) {
                prompt.append("英语教学建议：\n");
                prompt.append("- 创设真实的语言环境和交际情境\n");
                prompt.append("- 平衡听说读写四项技能的训练\n");
                prompt.append("- 注重语言实际运用和跨文化理解\n");
                prompt.append("- 采用多样化的教学活动提高学习兴趣\n\n");
            } else if (subject.contains("物理")) {
                prompt.append("物理教学建议：\n");
                prompt.append("- 结合实验和演示帮助理解物理概念\n");
                prompt.append("- 强调公式背后的物理意义\n");
                prompt.append("- 培养学生的科学思维和问题解决能力\n");
                prompt.append("- 联系实际生活现象解释物理原理\n\n");
            } else if (subject.contains("化学")) {
                prompt.append("化学教学建议：\n");
                prompt.append("- 通过实验观察和操作加深对化学反应的理解\n");
                prompt.append("- 强调化学用语和符号的规范使用\n");
                prompt.append("- 注重化学计算能力的培养\n");
                prompt.append("- 探讨化学与生活、环境的联系\n\n");
            } else if (subject.contains("生物")) {
                prompt.append("生物教学建议：\n");
                prompt.append("- 结合图片、模型和显微观察等直观教学手段\n");
                prompt.append("- 强调生物学概念和原理的理解\n");
                prompt.append("- 培养学生的科学探究能力\n");
                prompt.append("- 讨论生物学与健康、环境的关系\n\n");
            } else if (subject.contains("历史")) {
                prompt.append("历史教学建议：\n");
                prompt.append("- 通过历史事件和人物分析历史发展规律\n");
                prompt.append("- 培养学生的史料分析和历史思维能力\n");
                prompt.append("- 注重历史事件的因果关系和历史评价\n");
                prompt.append("- 引导学生从历史中汲取智慧\n\n");
            } else if (subject.contains("地理")) {
                prompt.append("地理教学建议：\n");
                prompt.append("- 结合地图、图表和实例进行地理现象分析\n");
                prompt.append("- 培养学生的空间思维和地理观察能力\n");
                prompt.append("- 探讨自然地理与人文地理的相互关系\n");
                prompt.append("- 关注地理环境与可持续发展\n\n");
            } else if (subject.contains("政治") || subject.contains("思想")) {
                prompt.append("思想政治教学建议：\n");
                prompt.append("- 结合时事热点和社会现实进行教学\n");
                prompt.append("- 培养学生的政治素养和价值判断能力\n");
                prompt.append("- 鼓励多角度思考和理性讨论\n");
                prompt.append("- 引导学生形成正确的世界观、人生观和价值观\n\n");
            }
        }
        
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
     * 从AI分析中提取学科信息
     */
    private String extractSubjectFromAnalysis(String aiResponse) {
        try {
            // 尝试从JSON中提取学科信息
            if (aiResponse.contains("\"subject\":")) {
                aiResponse = cleanJsonResponse(aiResponse);
                JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
                if (jsonResponse.containsKey("subject")) {
                    return jsonResponse.getStr("subject");
                }
            }
            
            // 尝试从文本中识别学科关键词
            String lowerCase = aiResponse.toLowerCase();
            if (lowerCase.contains("数学") || lowerCase.contains("函数") || lowerCase.contains("方程") || 
                lowerCase.contains("几何") || lowerCase.contains("概率") || lowerCase.contains("统计")) {
                return "数学";
            } else if (lowerCase.contains("语文") || lowerCase.contains("文学") || lowerCase.contains("阅读") || 
                      lowerCase.contains("写作") || lowerCase.contains("诗词") || lowerCase.contains("文言文")) {
                return "语文";
            } else if (lowerCase.contains("英语") || lowerCase.contains("词汇") || lowerCase.contains("语法") || 
                      lowerCase.contains("阅读理解") || lowerCase.contains("听力") || lowerCase.contains("口语")) {
                return "英语";
            } else if (lowerCase.contains("物理") || lowerCase.contains("力学") || lowerCase.contains("电学") || 
                      lowerCase.contains("热学") || lowerCase.contains("光学") || lowerCase.contains("功率")) {
                return "物理";
            } else if (lowerCase.contains("化学") || lowerCase.contains("元素") || lowerCase.contains("化合物") || 
                      lowerCase.contains("反应") || lowerCase.contains("酸碱") || lowerCase.contains("氧化还原")) {
                return "化学";
            } else if (lowerCase.contains("生物") || lowerCase.contains("细胞") || lowerCase.contains("遗传") || 
                      lowerCase.contains("进化") || lowerCase.contains("生态") || lowerCase.contains("分子")) {
                return "生物";
            } else if (lowerCase.contains("历史") || lowerCase.contains("朝代") || lowerCase.contains("战争") || 
                      lowerCase.contains("人物") || lowerCase.contains("事件") || lowerCase.contains("文明")) {
                return "历史";
            } else if (lowerCase.contains("地理") || lowerCase.contains("地形") || lowerCase.contains("气候") || 
                      lowerCase.contains("人口") || lowerCase.contains("资源") || lowerCase.contains("环境")) {
                return "地理";
            } else if (lowerCase.contains("政治") || lowerCase.contains("思想") || lowerCase.contains("哲学") || 
                      lowerCase.contains("法律") || lowerCase.contains("经济") || lowerCase.contains("社会")) {
                return "思想政治";
            }
        } catch (Exception e) {
            log.error("从AI分析中提取学科失败", e);
        }
        return null;
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