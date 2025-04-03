package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.TeachingPlanMapper;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.QuestionScoringResult;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.TeachingPlanGenerationService;
import com.xuesi.xuesisi.service.UserAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeachingPlanGenerationServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan> implements TeachingPlanGenerationService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Override
    public TeachingPlan generateTeachingPlan(QuestionBank questionBank, List<QuestionScoringResult> scoringResults, Long userAnswerId) {
        if (questionBank == null || scoringResults == null || scoringResults.isEmpty() || userAnswerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 构建提示词
        String prompt = buildTeachingPlanPrompt(questionBank, scoringResults);

        try {
            // 调用AI服务生成教学计划
            String aiResponse = deepSeekService.chat(prompt);
            log.info("AI响应内容: {}", aiResponse);
            
            // 清理AI响应中的markdown代码块标记和重复字段
            aiResponse = cleanJsonResponse(aiResponse);
            
            // 创建教学计划对象
            TeachingPlan teachingPlan = new TeachingPlan();
            teachingPlan.setQuestionBankId(questionBank.getId());
            teachingPlan.setUserId(questionBank.getUserId());
            teachingPlan.setSubject(questionBank.getSubject());
            
            // 设置用户答题记录ID
            teachingPlan.setUserAnswerId(userAnswerId);
            
            // 尝试解析JSON响应
            try {
                JSONObject responseJson = JSONUtil.parseObj(aiResponse);
                
                // 设置知识点列表 - 从题库问题中提取知识点
                List<String> knowledgePoints = new ArrayList<>();
                
                // 获取题库中的问题关联
                List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
                    new LambdaQueryWrapper<QuestionBankQuestion>()
                        .eq(QuestionBankQuestion::getQuestionBankId, questionBank.getId())
                );
                
                if (!questionBankQuestions.isEmpty()) {
                    // 获取所有题目ID
                    List<Long> questionIds = questionBankQuestions.stream()
                        .map(QuestionBankQuestion::getQuestionId)
                        .collect(Collectors.toList());
                    
                    // 获取题目详情
                    List<Question> questions = questionService.listByIds(questionIds);
                    
                    // 从题目中提取知识点标签
                    for (Question question : questions) {
                        if (StrUtil.isNotBlank(question.getTagsStr())) {
                            try {
                                JSONArray tags = JSONUtil.parseArray(question.getTagsStr());
                                for (int i = 0; i < tags.size(); i++) {
                                    knowledgePoints.add(tags.getStr(i));
                                }
                            } catch (Exception e) {
                                log.warn("解析问题标签失败: {}", question.getTagsStr());
                            }
                        }
                    }
                }
                
                // 去重并设置知识点
                knowledgePoints = knowledgePoints.stream().distinct().collect(Collectors.toList());
                teachingPlan.setKnowledgePoints(knowledgePoints);
                
                // 设置知识点分析
                if (responseJson.containsKey("knowledgeAnalysis")) {
                    teachingPlan.setKnowledgeAnalysis(responseJson.getJSONObject("knowledgeAnalysis"));
                } else {
                    JSONObject defaultAnalysis = new JSONObject();
                    defaultAnalysis.set("masteryLevel", "知识点掌握情况未分析");
                    defaultAnalysis.set("commonProblems", "未发现普遍问题");
                    defaultAnalysis.set("errorAnalysis", "未进行错误分析");
                    teachingPlan.setKnowledgeAnalysis(defaultAnalysis);
                }
                
                // 设置教学目标
                if (responseJson.containsKey("teachingObjectives")) {
                    teachingPlan.setTeachingObjectives(responseJson.getJSONArray("teachingObjectives"));
                } else {
                    JSONArray defaultObjectives = new JSONArray();
                    JSONObject objective = new JSONObject();
                    objective.set("type", "知识");
                    objective.set("content", "教学目标未生成");
                    defaultObjectives.add(objective);
                    teachingPlan.setTeachingObjectives(defaultObjectives);
                }
                
                // 设置教学安排
                if (responseJson.containsKey("teachingArrangement")) {
                    teachingPlan.setTeachingArrangement(responseJson.getJSONArray("teachingArrangement"));
                } else {
                    JSONArray defaultArrangement = new JSONArray();
                    JSONObject arrangement = new JSONObject();
                    arrangement.set("stage", "教学阶段");
                    arrangement.set("duration", "时长");
                    arrangement.set("content", "教学安排未生成");
                    arrangement.set("methods", new JSONArray());
                    arrangement.set("materials", new JSONArray());
                    arrangement.set("activities", new JSONArray());
                    defaultArrangement.add(arrangement);
                    teachingPlan.setTeachingArrangement(defaultArrangement);
                }
                
                // 设置预期成果
                if (responseJson.containsKey("expectedOutcomes")) {
                    teachingPlan.setExpectedOutcomes(responseJson.getJSONObject("expectedOutcomes"));
                } else {
                    JSONObject defaultOutcomes = new JSONObject();
                    defaultOutcomes.set("knowledge", "预期知识成果未生成");
                    defaultOutcomes.set("skills", "预期技能成果未生成");
                    defaultOutcomes.set("attitudes", "预期态度成果未生成");
                    teachingPlan.setExpectedOutcomes(defaultOutcomes);
                }
                
                // 设置评估方法
                if (responseJson.containsKey("evaluationMethods")) {
                    teachingPlan.setEvaluationMethods(responseJson.getJSONArray("evaluationMethods"));
                } else {
                    JSONArray defaultMethods = new JSONArray();
                    JSONObject method = new JSONObject();
                    method.set("type", "评估方法");
                    method.set("description", "评估方法未生成");
                    defaultMethods.add(method);
                    teachingPlan.setEvaluationMethods(defaultMethods);
                }
            } catch (Exception e) {
                log.error("解析AI响应失败，使用默认值", e);
                // 设置默认值
                JSONObject defaultAnalysis = new JSONObject();
                defaultAnalysis.set("masteryLevel", "知识点掌握情况未分析");
                defaultAnalysis.set("commonProblems", "未发现普遍问题");
                defaultAnalysis.set("errorAnalysis", "未进行错误分析");
                teachingPlan.setKnowledgeAnalysis(defaultAnalysis);
                
                JSONArray defaultObjectives = new JSONArray();
                JSONObject objective = new JSONObject();
                objective.set("type", "知识");
                objective.set("content", "教学目标未生成");
                defaultObjectives.add(objective);
                teachingPlan.setTeachingObjectives(defaultObjectives);
                
                JSONArray defaultArrangement = new JSONArray();
                JSONObject arrangement = new JSONObject();
                arrangement.set("stage", "教学阶段");
                arrangement.set("duration", "时长");
                arrangement.set("content", "教学安排未生成");
                arrangement.set("methods", new JSONArray());
                arrangement.set("materials", new JSONArray());
                arrangement.set("activities", new JSONArray());
                defaultArrangement.add(arrangement);
                teachingPlan.setTeachingArrangement(defaultArrangement);
                
                JSONObject defaultOutcomes = new JSONObject();
                defaultOutcomes.set("knowledge", "预期知识成果未生成");
                defaultOutcomes.set("skills", "预期技能成果未生成");
                defaultOutcomes.set("attitudes", "预期态度成果未生成");
                teachingPlan.setExpectedOutcomes(defaultOutcomes);
                
                JSONArray defaultMethods = new JSONArray();
                JSONObject method = new JSONObject();
                method.set("type", "评估方法");
                method.set("description", "评估方法未生成");
                defaultMethods.add(method);
                teachingPlan.setEvaluationMethods(defaultMethods);

                // 设置默认知识点列表
                teachingPlan.setKnowledgePoints("[]");
            }

            // 保存教学计划
            save(teachingPlan);
            return teachingPlan;
        } catch (Exception e) {
            log.error("生成教学计划失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成教学计划失败");
        }
    }

    private String buildTeachingPlanPrompt(QuestionBank questionBank, List<QuestionScoringResult> scoringResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下信息生成一个教学计划，并严格按照指定的JSON格式返回：\n\n");
        prompt.append("题库信息：\n");
        prompt.append("- 学科：").append(questionBank.getSubject()).append("\n");
        prompt.append("- 题目数量：").append(questionBank.getQuestionCount()).append("\n\n");
        
        prompt.append("评分结果：\n");
        for (QuestionScoringResult result : scoringResults) {
            prompt.append("- 题目ID：").append(result.getQuestionId()).append("\n");
            prompt.append("  得分：").append(result.getScore()).append("\n");
            prompt.append("  分析：").append(result.getAnalysis()).append("\n\n");
        }
        
        prompt.append("请严格按照以下JSON格式返回（注意：所有字段都必须存在，且格式必须完全匹配）：\n");
        prompt.append("{\n");
        prompt.append("  \"knowledgeAnalysis\": {\n");
        prompt.append("    \"masteryLevel\": \"知识点掌握情况\",\n");
        prompt.append("    \"commonProblems\": \"普遍存在的问题\",\n");
        prompt.append("    \"errorAnalysis\": \"错误原因分析\"\n");
        prompt.append("  },\n");
        prompt.append("  \"teachingObjectives\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"知识/能力/情感\",\n");
        prompt.append("      \"content\": \"具体目标\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"teachingArrangement\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"教学阶段\",\n");
        prompt.append("      \"duration\": \"时长\",\n");
        prompt.append("      \"content\": \"具体内容\",\n");
        prompt.append("      \"methods\": [\"教学方法\"],\n");
        prompt.append("      \"materials\": [\"教学资源\"],\n");
        prompt.append("      \"activities\": [\"教学活动\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"expectedOutcomes\": {\n");
        prompt.append("    \"knowledge\": \"知识掌握目标\",\n");
        prompt.append("    \"skills\": \"技能提升目标\",\n");
        prompt.append("    \"attitudes\": \"态度改善目标\"\n");
        prompt.append("  },\n");
        prompt.append("  \"evaluationMethods\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"评估方法类型\",\n");
        prompt.append("      \"description\": \"具体描述\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("1. 必须严格按照上述JSON格式返回\n");
        prompt.append("2. 所有字段都必须存在\n");
        prompt.append("3. 不要添加任何额外的字段\n");
        prompt.append("4. 确保所有字符串都使用双引号\n");
        prompt.append("5. 确保所有数组和对象都正确闭合\n");
        prompt.append("6. 不要在JSON中包含任何注释或说明文字\n");
        prompt.append("7. 确保所有数值都是有效的JSON格式\n");
        prompt.append("8. 不要使用任何特殊字符或转义字符\n");

        return prompt.toString();
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
        if (response == null) {
            return "{}";
        }
        // 移除markdown代码块标记
        response = response.replaceAll("```json\\s*", "")
                         .replaceAll("```\\s*", "")
                         .trim();
        // 确保响应以{开头
        if (!response.startsWith("{")) {
            response = "{" + response;
        }
        // 确保响应以}结尾
        if (!response.endsWith("}")) {
            response = response + "}";
        }
        return response;
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
    private String buildPrompt(QuestionBank questionBank, UserAnswerVO userAnswerVO, List<String> weakKnowledgePoints) {
        StringBuilder prompt = new StringBuilder();
        
        // 添加基本信息
        prompt.append("你是一位经验丰富的教师，请根据以下信息生成一份详细的教学计划：\n\n");
        prompt.append("1. 题库信息：\n");
        prompt.append("   - 名称：").append(questionBank.getTitle()).append("\n");
        prompt.append("   - 类型：").append(questionBank.getQuestionBankType() == 0 ? "单选题" : 
                                          questionBank.getQuestionBankType() == 1 ? "多选题" :
                                          questionBank.getQuestionBankType() == 2 ? "填空题" : "简答题").append("\n");
        prompt.append("   - 评分策略：").append(questionBank.getScoringStrategy() == 0 ? "自定义" : "AI").append("\n");
        prompt.append("   - 总分：").append(questionBank.getTotalScore()).append("\n");
        prompt.append("   - 及格分：").append(questionBank.getPassScore()).append("\n");
        prompt.append("   - 学科：").append(questionBank.getSubject()).append("\n");
        
        // 添加题目信息
        prompt.append("\n2. 答题信息：\n");
        prompt.append("   - 得分：").append(userAnswerVO.getResultScore()).append("\n");
        prompt.append("   - 答案：").append(userAnswerVO.getChoices()).append("\n");
        prompt.append("   - 结果描述：").append(userAnswerVO.getResultDesc()).append("\n");
        
        // 添加薄弱知识点
        prompt.append("\n3. 学生薄弱知识点：\n");
        for (String point : weakKnowledgePoints) {
            prompt.append("   - ").append(point).append("\n");
        }
        
        // 添加教学计划要求
        prompt.append("\n请生成一份详细的教学计划，包含以下内容：\n");
        prompt.append("1. 知识点分析：\n");
        prompt.append("   - 分析每个知识点的掌握情况\n");
        prompt.append("   - 找出学生普遍存在的问题\n");
        prompt.append("   - 分析错误原因\n\n");
        
        prompt.append("2. 教学目标：\n");
        prompt.append("   - 知识目标：具体要掌握的知识点\n");
        prompt.append("   - 能力目标：要培养的能力\n");
        prompt.append("   - 情感目标：学习态度和习惯的培养\n\n");
        
        prompt.append("3. 教学安排：\n");
        prompt.append("   - 课前准备：教师和学生需要准备的内容\n");
        prompt.append("   - 课堂流程：\n");
        prompt.append("     * 导入环节（5分钟）：复习旧知，引入新课\n");
        prompt.append("     * 知识讲解（20分钟）：重点讲解薄弱知识点\n");
        prompt.append("     * 错题分析（15分钟）：分析典型错题，总结解题方法\n");
        prompt.append("     * 练习巩固（15分钟）：针对性练习\n");
        prompt.append("     * 总结提升（5分钟）：归纳总结，布置作业\n");
        prompt.append("   - 教学方法：具体使用的教学方法\n");
        prompt.append("   - 教学资源：需要使用的教具、课件等\n\n");
        
        prompt.append("4. 错题解决方案：\n");
        prompt.append("   - 针对每道错题的具体解决方案\n");
        prompt.append("   - 解题思路和方法的指导\n");
        prompt.append("   - 常见错误的预防措施\n\n");
        
        prompt.append("5. 预期成果：\n");
        prompt.append("   - 知识掌握程度\n");
        prompt.append("   - 能力提升目标\n");
        prompt.append("   - 学习态度改善\n\n");
        
        prompt.append("6. 评估方法：\n");
        prompt.append("   - 课堂表现评估\n");
        prompt.append("   - 作业完成情况\n");
        prompt.append("   - 测试成绩评估\n");
        
        prompt.append("\n请以JSON格式返回，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"knowledgeAnalysis\": {\n");
        prompt.append("    \"masteryLevel\": \"知识点掌握情况\",\n");
        prompt.append("    \"commonProblems\": \"普遍存在的问题\",\n");
        prompt.append("    \"errorAnalysis\": \"错误原因分析\"\n");
        prompt.append("  },\n");
        prompt.append("  \"teachingDesign\": {\n");
        prompt.append("    \"teachingObjectives\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"type\": \"知识/能力/情感\",\n");
        prompt.append("        \"content\": \"具体目标\"\n");
        prompt.append("      }\n");
        prompt.append("    ],\n");
        prompt.append("    \"teachingArrangement\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"stage\": \"教学阶段\",\n");
        prompt.append("        \"duration\": \"时长\",\n");
        prompt.append("        \"content\": \"具体内容\",\n");
        prompt.append("        \"methods\": [\"教学方法\"],\n");
        prompt.append("        \"materials\": [\"教学资源\"],\n");
        prompt.append("        \"activities\": [\"教学活动\"]\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"errorSolutions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"questionId\": \"题目ID\",\n");
        prompt.append("      \"problem\": \"问题描述\",\n");
        prompt.append("      \"solution\": \"解决方案\",\n");
        prompt.append("      \"prevention\": \"预防措施\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"expectedOutcomes\": {\n");
        prompt.append("    \"knowledge\": \"知识掌握目标\",\n");
        prompt.append("    \"ability\": \"能力提升目标\",\n");
        prompt.append("    \"attitude\": \"态度改善目标\"\n");
        prompt.append("  },\n");
        prompt.append("  \"evaluationMethods\": {\n");
        prompt.append("    \"classroom\": \"课堂表现评估\",\n");
        prompt.append("    \"homework\": \"作业评估\",\n");
        prompt.append("    \"test\": \"测试评估\"\n");
        prompt.append("  }\n");
        prompt.append("}");
        
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
            
            // 记录原始响应以便调试
            log.debug("AI响应原始内容: {}", aiResponse);
            
            // 尝试修复常见的JSON格式问题
            aiResponse = aiResponse.replaceAll("(?m),\\s*}", "}")  // 移除对象末尾多余的逗号
                                 .replaceAll("(?m),\\s*]", "]")   // 移除数组末尾多余的逗号
                                 .replaceAll("(?m)\\s*,\\s*}", "}")  // 修复对象末尾的逗号
                                 .replaceAll("(?m)\\s*,\\s*]", "]"); // 修复数组末尾的逗号
            
            // 确保JSON格式正确
            if (!aiResponse.startsWith("{")) {
                aiResponse = "{" + aiResponse;
            }
            if (!aiResponse.endsWith("}")) {
                aiResponse = aiResponse + "}";
            }
            
            // 解析JSON
        JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
        
            // 创建教学计划对象
        TeachingPlan teachingPlan = new TeachingPlan();
        teachingPlan.setQuestionBankId(questionBank.getId());
        teachingPlan.setUserId(questionBank.getUserId());
            teachingPlan.setUserAnswerId(questionBank.getUserId());
            
            // 设置学科
            teachingPlan.setSubject(questionBank.getSubject());
            
            // 设置知识点列表
            List<String> knowledgePoints = new ArrayList<>();
            List<QuestionVO> questions = questionBankService.getQuestionsByBankId(questionBank.getId());
            for (QuestionVO question : questions) {
                if (question.getTags() != null) {
                    JSONArray tags = JSONUtil.parseArray(question.getTags());
                    for (int i = 0; i < tags.size(); i++) {
                        knowledgePoints.add(tags.getStr(i));
                    }
                }
            }
            // 去重
            knowledgePoints = knowledgePoints.stream().distinct().collect(Collectors.toList());
            teachingPlan.setKnowledgePoints(knowledgePoints);
            
            // 设置知识点分析
            if (jsonResponse.containsKey("knowledgeAnalysis")) {
                teachingPlan.setKnowledgeAnalysis(jsonResponse.get("knowledgeAnalysis"));
            } else {
                log.warn("AI响应中缺少knowledgeAnalysis字段");
                JSONObject defaultAnalysis = new JSONObject();
                defaultAnalysis.set("masteryLevel", "知识点掌握情况未分析");
                defaultAnalysis.set("commonProblems", "未发现普遍问题");
                defaultAnalysis.set("errorAnalysis", "未进行错误分析");
                teachingPlan.setKnowledgeAnalysis(defaultAnalysis);
            }
            
            // 设置教学设计
            if (jsonResponse.containsKey("teachingDesign")) {
                JSONObject teachingDesign = jsonResponse.getJSONObject("teachingDesign");
                if (teachingDesign != null) {
                    if (teachingDesign.containsKey("teachingObjectives")) {
                        teachingPlan.setTeachingObjectives(teachingDesign.get("teachingObjectives"));
                    } else {
                        log.warn("teachingDesign中缺少teachingObjectives字段");
                        JSONArray defaultObjectives = new JSONArray();
                        JSONObject objective = new JSONObject();
                        objective.set("type", "知识");
                        objective.set("content", "教学目标未生成");
                        defaultObjectives.add(objective);
                        teachingPlan.setTeachingObjectives(defaultObjectives);
                    }
                    
                    if (teachingDesign.containsKey("teachingArrangement")) {
                        teachingPlan.setTeachingArrangement(teachingDesign.get("teachingArrangement"));
                    } else {
                        log.warn("teachingDesign中缺少teachingArrangement字段");
                        JSONArray defaultArrangement = new JSONArray();
                        JSONObject arrangement = new JSONObject();
                        arrangement.set("stage", "教学阶段");
                        arrangement.set("duration", "时长");
                        arrangement.set("content", "教学安排未生成");
                        arrangement.set("methods", new JSONArray());
                        arrangement.set("materials", new JSONArray());
                        arrangement.set("activities", new JSONArray());
                        defaultArrangement.add(arrangement);
                        teachingPlan.setTeachingArrangement(defaultArrangement);
                    }
                }
            } else {
                log.warn("AI响应中缺少teachingDesign字段");
                JSONArray defaultObjectives = new JSONArray();
                JSONObject objective = new JSONObject();
                objective.set("type", "知识");
                objective.set("content", "教学目标未生成");
                defaultObjectives.add(objective);
                teachingPlan.setTeachingObjectives(defaultObjectives);
                
                JSONArray defaultArrangement = new JSONArray();
                JSONObject arrangement = new JSONObject();
                arrangement.set("stage", "教学阶段");
                arrangement.set("duration", "时长");
                arrangement.set("content", "教学安排未生成");
                arrangement.set("methods", new JSONArray());
                arrangement.set("materials", new JSONArray());
                arrangement.set("activities", new JSONArray());
                defaultArrangement.add(arrangement);
                teachingPlan.setTeachingArrangement(defaultArrangement);
            }
            
            // 设置预期成果和评估方法
            if (jsonResponse.containsKey("expectedOutcomes")) {
                teachingPlan.setExpectedOutcomes(jsonResponse.get("expectedOutcomes"));
            } else {
                log.warn("AI响应中缺少expectedOutcomes字段");
                JSONObject defaultOutcomes = new JSONObject();
                defaultOutcomes.set("knowledge", "知识掌握目标未设定");
                defaultOutcomes.set("ability", "能力提升目标未设定");
                defaultOutcomes.set("attitude", "态度改善目标未设定");
                teachingPlan.setExpectedOutcomes(defaultOutcomes);
            }
            
            if (jsonResponse.containsKey("evaluationMethods")) {
                teachingPlan.setEvaluationMethods(jsonResponse.get("evaluationMethods"));
            } else {
                log.warn("AI响应中缺少evaluationMethods字段");
                JSONObject defaultMethods = new JSONObject();
                defaultMethods.set("classroom", "课堂表现评估方法未设定");
                defaultMethods.set("homework", "作业评估方法未设定");
                defaultMethods.set("test", "测试评估方法未设定");
                teachingPlan.setEvaluationMethods(defaultMethods);
            }
        
        return teachingPlan;
        } catch (Exception e) {
            log.error("解析教学计划失败，原始响应: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解析教学计划失败：" + e.getMessage());
        }
    }
} 