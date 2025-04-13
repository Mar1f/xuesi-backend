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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

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
        log.info("开始生成教学计划...");
        
        try {
            // 构建教学计划提示词
            String prompt = buildTeachingPlanPrompt(questionBank, scoringResults);
            
            // 调用DeepSeek API获取响应
            String aiResponse = deepSeekService.chat(prompt);
            
            // 清理和修复JSON
            String cleanedResponse = cleanJsonResponse(aiResponse); 
            log.debug("修复后的JSON: {}", cleanedResponse);
            
            // 解析并生成教学计划
            TeachingPlan teachingPlan = new TeachingPlan();
            teachingPlan.setQuestionBankId(questionBank.getId());
            teachingPlan.setUserId(questionBank.getUserId());
            teachingPlan.setUserAnswerId(userAnswerId);
            
            // 直接设置题库中的原始学科，保留教育阶段信息（如"高中数学"）
            teachingPlan.setSubject(questionBank.getSubject());
            teachingPlan.setTitle(questionBank.getTitle()); // Set title from QuestionBank
            
            // 解析AI响应并填充教学计划
            generateTeachingPlan(cleanedResponse, teachingPlan);
            
            // 保存到数据库
            boolean success = save(teachingPlan);
            if (!success) {
                throw new RuntimeException("保存教学计划失败");
            }
            
            return teachingPlan;
            
        } catch (Exception e) {
            log.error("生成教学计划失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成教学计划失败: " + e.getMessage());
        }
    }

    private String buildTeachingPlanPrompt(QuestionBank questionBank, List<QuestionScoringResult> scoringResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下信息生成一个详细、全面的教学计划，并严格按照指定的JSON格式返回。请确保每个部分都有足够的细节和深度，内容至少包含800-1000字的详细说明：\n\n");
        prompt.append("题库信息：\n");
        prompt.append("- 学科：").append(questionBank.getSubject()).append("\n");
        prompt.append("- 题目数量：").append(questionBank.getQuestionCount()).append("\n\n");
        
        prompt.append("评分结果：\n");
        for (QuestionScoringResult result : scoringResults) {
            prompt.append("- 题目ID：").append(result.getQuestionId()).append("\n");
            prompt.append("  得分：").append(result.getScore()).append("\n");
            prompt.append("  分析：").append(result.getAnalysis()).append("\n\n");
        }
        
        prompt.append("请严格按照以下JSON格式返回（注意：所有字段都必须存在，且格式必须完全匹配）。生成的内容应该非常详细且实用，适合教师直接参考和使用：\n");
        prompt.append("{\n");
        prompt.append("  \"knowledgeAnalysis\": {\n");
        prompt.append("    \"masteryLevel\": \"请提供详细的知识点掌握情况分析，包括学生对各个知识点的掌握程度，根据得分结果识别出的强项和弱项。至少100字的深入分析。\",\n");
        prompt.append("    \"commonProblems\": \"详细描述学生在学习过程中普遍存在的问题，包括概念理解偏差、解题方法不当、思维习惯问题等多个方面。至少150字的问题剖析。\",\n");
        prompt.append("    \"errorAnalysis\": \"针对每一个错误提供深入的原因分析，包括认知障碍、方法缺陷、思维定势等因素，并提供具体的改进建议。至少150字的详细分析。\"\n");
        prompt.append("  },\n");
        prompt.append("  \"teachingObjectives\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"知识\",\n");
        prompt.append("      \"content\": \"详细列出本次教学需要掌握的具体知识点，包括概念、原理、公式、方法等，至少包含5-8个明确的知识目标。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"能力\",\n");
        prompt.append("      \"content\": \"详细描述通过本次教学需要培养的能力，包括分析能力、解决问题能力、应用能力等，至少包含5-8个具体能力目标。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"情感\",\n");
        prompt.append("      \"content\": \"详细阐述本次教学在情感、态度、价值观方面的培养目标，包括学习兴趣、自信心、合作精神等，至少包含3-5个情感目标。\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"teachingArrangement\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"导入阶段\",\n");
        prompt.append("      \"duration\": \"10-15分钟\",\n");
        prompt.append("      \"content\": \"详细描述如何导入新课，包括复习旧知识的方法、引入新知识的情境设计、激发学生兴趣的具体活动等。至少150字的具体描述。\",\n");
        prompt.append("      \"methods\": [\"请列出3-5种适合该阶段的教学方法\"],\n");
        prompt.append("      \"materials\": [\"请列出3-5种需要准备的教学资源和材料\"],\n");
        prompt.append("      \"activities\": [\"请列出3-5个具体的教学活动设计\"]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"新知讲解阶段\",\n");
        prompt.append("      \"duration\": \"20-25分钟\",\n");
        prompt.append("      \"content\": \"详细描述新知识教学的具体内容和流程，包括重点难点的讲解方法、示例选择、推导过程等。至少200字的详细教学内容。\",\n");
        prompt.append("      \"methods\": [\"请列出3-5种适合该阶段的教学方法\"],\n");
        prompt.append("      \"materials\": [\"请列出3-5种需要准备的教学资源和材料\"],\n");
        prompt.append("      \"activities\": [\"请列出3-5个具体的教学活动设计\"]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"巩固练习阶段\",\n");
        prompt.append("      \"duration\": \"15-20分钟\",\n");
        prompt.append("      \"content\": \"详细设计针对性的练习活动，包括基础练习、提高练习、拓展练习等不同层次，以及具体的习题设计和解题指导。至少200字的详细内容。\",\n");
        prompt.append("      \"methods\": [\"请列出3-5种适合该阶段的教学方法\"],\n");
        prompt.append("      \"materials\": [\"请列出3-5种需要准备的教学资源和材料\"],\n");
        prompt.append("      \"activities\": [\"请列出3-5个具体的教学活动设计\"]\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"总结反馈阶段\",\n");
        prompt.append("      \"duration\": \"10-15分钟\",\n");
        prompt.append("      \"content\": \"详细描述如何引导学生总结本节课的知识要点、解决问题的方法和技巧，以及如何进行有效的反馈和评价。至少150字的详细内容。\",\n");
        prompt.append("      \"methods\": [\"请列出3-5种适合该阶段的教学方法\"],\n");
        prompt.append("      \"materials\": [\"请列出3-5种需要准备的教学资源和材料\"],\n");
        prompt.append("      \"activities\": [\"请列出3-5个具体的教学活动设计\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"expectedOutcomes\": {\n");
        prompt.append("    \"knowledge\": \"详细描述学生在知识层面预期达到的掌握程度，包括基础知识、核心概念、关键方法等方面的具体表现。至少150字的详细描述。\",\n");
        prompt.append("    \"skills\": \"详细描述学生在技能层面预期达到的熟练程度，包括解题能力、应用能力、分析能力等方面的具体表现。至少150字的详细描述。\",\n");
        prompt.append("    \"attitudes\": \"详细描述学生在态度情感层面预期达到的转变，包括学习兴趣、学习习惯、团队合作等方面的具体表现。至少100字的详细描述。\"\n");
        prompt.append("  },\n");
        prompt.append("  \"evaluationMethods\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"过程性评价\",\n");
        prompt.append("      \"description\": \"详细描述如何通过课堂观察、提问、小组活动等方式进行过程性评价，包括具体的评价指标、方法和记录方式。至少100字的详细说明。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"形成性评价\",\n");
        prompt.append("      \"description\": \"详细描述如何通过作业、测验、项目等方式进行形成性评价，包括具体的评价工具设计、评分标准和反馈方式。至少100字的详细说明。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"总结性评价\",\n");
        prompt.append("      \"description\": \"详细描述如何通过单元测试、期末考试等方式进行总结性评价，包括具体的试题设计思路、难度分布和评分标准。至少100字的详细说明。\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"differentiatedInstruction\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"level\": \"基础水平学生\",\n");
        prompt.append("      \"strategies\": \"详细描述针对基础薄弱学生的个性化教学策略，包括具体的辅导方法、材料调整和目标设定。至少100字的详细说明。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"level\": \"中等水平学生\",\n");
        prompt.append("      \"strategies\": \"详细描述针对中等水平学生的个性化教学策略，包括具体的深化方法、拓展材料和挑战设计。至少100字的详细说明。\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"level\": \"优秀水平学生\",\n");
        prompt.append("      \"strategies\": \"详细描述针对优秀学生的个性化教学策略，包括具体的探究任务、创新项目和自主学习指导。至少100字的详细说明。\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("1. 必须严格按照上述JSON格式返回，内容须非常详细且具有实用性\n");
        prompt.append("2. 所有字段都必须存在，每个描述性内容至少应包含指定字数的详细说明\n");
        prompt.append("3. 不要添加任何额外的字段，但每个字段内容应该丰富、具体、可操作\n");
        prompt.append("4. 确保所有字符串都使用双引号，内容应包含具体的教学建议而非空泛表述\n");
        prompt.append("5. 确保所有数组和对象都正确闭合，列表项应具体且有针对性\n");
        prompt.append("6. 不要在JSON中包含任何注释或说明文字，但各部分内容应自成体系\n");
        prompt.append("7. 教学计划应反映评分结果中暴露的问题和知识点，做到有的放矢\n");
        prompt.append("8. 所有教学设计应符合现代教育理念，注重学生的主动参与和思维发展\n");

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
     * 修复缺少的逗号
     */
    private String fixMissingCommas(String json) {
        // 修复字段之间缺少逗号的情况
        String fixed = json;
        
        // 修复特定错误：在JSON字符串1312位置附近的问题（根据错误日志）
        // 匹配content后面缺少逗号的场景
        Pattern contentPattern = Pattern.compile("(\"content\"\\s*:\\s*\"[^\"]*\")\\s*(\"methods\")");
        Matcher contentMatcher = contentPattern.matcher(fixed);
        if (contentMatcher.find()) {
            fixed = contentMatcher.replaceAll("$1, $2");
        }
        
        // 更一般的情况：任何字段后面缺少逗号的情况
        fixed = fixed.replaceAll("(\"[^\"]+\"\\s*:\\s*\"[^\"]*\")\\s*(\"[^\"]+\")", "$1, $2");
        
        // 处理数组中缺少逗号的情况
        fixed = fixed.replaceAll("(\\])\\s*(\\{)", "$1, $2");
        
        return fixed;
    }
    
    /**
     * 手动修复教学安排中的特定问题
     */
    private String manuallyFixTeachingArrangement(String json) {
        String[] lines = json.split("\n");
        StringBuilder fixed = new StringBuilder();
        
        boolean inTeachingArrangement = false;
        boolean expectingComma = false;
        
        for (String line : lines) {
            // 检测是否在教学安排数组中
            if (line.contains("\"teachingArrangement\"")) {
                inTeachingArrangement = true;
            } else if (inTeachingArrangement && line.contains("]")) {
                inTeachingArrangement = false;
            }
            
            // 如果在教学安排数组中，检查是否缺少逗号
            if (inTeachingArrangement) {
                // 如果前一行是content字段，当前行是methods字段，但中间缺少逗号
                if (expectingComma && line.trim().startsWith("\"methods\"")) {
                    // 添加逗号
                    fixed.append(",\n").append(line);
                    expectingComma = false;
                    continue;
                }
                
                // 检测是否是content字段
                expectingComma = line.contains("\"content\"") && !line.trim().endsWith(",");
            }
            
            fixed.append(line).append("\n");
        }
        
        return fixed.toString();
    }
    
    /**
     * 手动修复AI响应中的特定JSON错误
     */
    private String fixSpecificAIResponseErrors(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        // 替换错误的 'content' 对象格式，特别是缺少逗号的情况
        String fixed = json;
        
        // 修复特定问题：在教学安排数组中特定位置的错误
        fixed = fixed.replace("\"content\": \"1. 通过复习三角函数的基本概念，引导学生回忆并理解角度和弧度的转换关系；2. 通过展示实际生活中的三角函数应用案例，激发学生的学习兴趣；3. 通过提问和讨论的方式，了解学生对三角函数的现有认知和理解。\"\n      \"methods\"",
                            "\"content\": \"1. 通过复习三角函数的基本概念，引导学生回忆并理解角度和弧度的转换关系；2. 通过展示实际生活中的三角函数应用案例，激发学生的学习兴趣；3. 通过提问和讨论的方式，了解学生对三角函数的现有认知和理解。\",\n      \"methods\"");
        
        // 修复其他类似错误，通过使用split和手动添加逗号
        String[] sections = fixed.split("\"stage\":");
        StringBuilder result = new StringBuilder();
        
        if (sections.length > 0) {
            result.append(sections[0]);
            
            for (int i = 1; i < sections.length; i++) {
                result.append("\"stage\":");
                
                // 在每个section中修复缺少逗号的情况
                String section = sections[i];
                section = section.replace("\"\n      \"", "\",\n      \"");
                
                result.append(section);
            }
            
            fixed = result.toString();
        }
        
        return fixed;
    }
    
    /**
     * 修复JSON中不完整的内容
     */
    private String fixTruncatedJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        
        // 1. 检查是否有被截断的expectedOutcomes
        if (json.contains("\"expectedOutcomes\"")) {
            // 处理expectedOutcomes格式错误的情况（是数组而不是对象）
            Pattern expectOutcomesPattern = Pattern.compile(
                "\"expectedOutcomes\"\\s*:\\s*\\[\\s*\\{\\s*\"type\"\\s*:\\s*\"知识\"", 
                Pattern.DOTALL
            );
            Matcher expectOutcomesMatcher = expectOutcomesPattern.matcher(json);
            if (expectOutcomesMatcher.find()) {
                // 尝试将数组格式转换为对象格式
                json = json.replaceAll(
                    "\"expectedOutcomes\"\\s*:\\s*\\[\\s*\\{\\s*\"type\"\\s*:\\s*\"知识\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*,?\\s*\\{\\s*\"type\"\\s*:\\s*\"能力\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*,?\\s*\\{\\s*\"type\"\\s*:\\s*\"情感\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}",
                    "\"expectedOutcomes\":{\"knowledge\":\"$1\""
                );
                
                // 检查是否有被截断的内容
                Pattern contentPattern = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]*)\"(?!\\s*[,}\\]])", Pattern.DOTALL);
                Matcher contentMatcher = contentPattern.matcher(json);
                if (contentMatcher.find()) {
                    // 处理被截断的内容
                    String truncatedContent = contentMatcher.group(1);
                    json = json.replace(
                        truncatedContent,
                        truncatedContent + "\""
                    );
                }
                
                // 确保expectedOutcomes对象正确关闭
                if (!json.contains("expectedOutcomes\":{") || !json.contains("\"}")) {
                    json = json.replaceAll("(\"expectedOutcomes\"\\s*:\\s*\\{[^}]*?)(?=\\s*\\,|\\s*\\}|\\s*$)", "$1}");
                }
            }
            
            // 处理知识内容被截断的情况
            Pattern truncatedKnowledgePattern = Pattern.compile(
                "\"content\"\\s*:\\s*\"学生能够[^\"]*$", 
                Pattern.DOTALL
            );
            Matcher truncatedKnowledgeMatcher = truncatedKnowledgePattern.matcher(json);
            if (truncatedKnowledgeMatcher.find()) {
                // 添加适当的结束符号
                String truncatedContent = truncatedKnowledgeMatcher.group(0);
                json = json.replace(truncatedContent, truncatedContent + "\"");
                
                // 如果是expectedOutcomes数组中的最后一个元素被截断
                if (json.contains("\"expectedOutcomes\"") && !json.contains("\"}]")) {
                    json = json + "}]}";
                }
                
                // 修复可能的不正确闭合
                json = json.replaceAll("\\}\\}\\}\\}\\]\\}$", "\"}}]}");
            }
        }
        
        // 2. 检查并修复differentiatedInstruction部分
        if (json.contains("\"differentiatedInstruction\"")) {
            // 特别处理由日志中发现的问题：在"优秀水平学生"策略内容被截断的情况
            Pattern truncatedPattern = Pattern.compile(
                "\"level\"\\s*:\\s*\"优秀水平学生\"\\s*,\\s*\"strategies\"\\s*:\\s*\"([^\"]*)$", 
                Pattern.DOTALL
            );
            Matcher truncatedMatcher = truncatedPattern.matcher(json);
            if (truncatedMatcher.find()) {
                // 找到被截断的内容，进行修复
                String truncatedContent = truncatedMatcher.group(1);
                // 添加引号和大括号结束
                json = json.replace(
                    truncatedContent, 
                    truncatedContent + "\"}]}"
                );
                
                // 清理可能存在的额外字符
                json = json.replaceAll("\\]\\]\\]\\}+$", "]}}");
                json = json.replaceAll("\\}\\}\\}+$", "}}");
            }
            
            // 检查是否在differentiatedInstruction部分被截断
            Pattern diffPattern = Pattern.compile(
                "(\"differentiatedInstruction\"\\s*:\\s*\\[.*?)\\]\\]\\]\\}$", 
                Pattern.DOTALL
            );
            Matcher diffMatcher = diffPattern.matcher(json);
            if (diffMatcher.find()) {
                // 修复错误的结束部分
                return diffMatcher.group(1) + "]}";
            }
        }
        
        // 3. 检查JSON是否完整
        int openBraces = 0, closeBraces = 0;
        int openBrackets = 0, closeBrackets = 0;
        for (char c : json.toCharArray()) {
            if (c == '{') openBraces++;
            else if (c == '}') closeBraces++;
            else if (c == '[') openBrackets++;
            else if (c == ']') closeBrackets++;
        }
        
        // 4. 如果JSON不完整，尝试修复
        StringBuilder fixed = new StringBuilder(json);
        
        // 添加缺少的大括号
        while (closeBraces < openBraces) {
            fixed.append("}");
            closeBraces++;
        }
        
        // 添加缺少的方括号
        while (closeBrackets < openBrackets) {
            fixed.append("]");
            closeBrackets++;
        }
        
        // 5. 修复过多的闭合
        String result = fixed.toString();
        result = result.replaceAll("\\]\\]\\]\\}$", "]}}");
        result = result.replaceAll("\\}\\}\\}$", "}}");
        
        // 6. 对一些常见字段格式做最终修复
        // 修复expectedOutcomes对象格式
        result = result.replaceAll("\"expectedOutcomes\"\\s*:\\s*\\[\\s*\\{\\s*\"type\"\\s*:\\s*\"知识\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*,?\\s*\\{\\s*\"type\"\\s*:\\s*\"能力\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}\\s*,?\\s*\\{\\s*\"type\"\\s*:\\s*\"情感\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"\\s*\\}", 
                            "\"expectedOutcomes\":{\"knowledge\":\"$1\",\"skills\":\"$2\",\"attitudes\":\"$3\"}");
        
        // 修复特定问题: 修复未闭合的differentiatedInstruction对象和数组
        if (result.contains("\"differentiatedInstruction\"") && !result.contains("\"}]")) {
            // 尝试找到最后一个策略条目
            Pattern lastStrategyPattern = Pattern.compile(
                "(\"strategies\"\\s*:\\s*\"[^\"]*)(\"\\s*\\}\\s*)?$", 
                Pattern.DOTALL
            );
            Matcher lastStrategyMatcher = lastStrategyPattern.matcher(result);
            if (lastStrategyMatcher.find()) {
                String lastPart = lastStrategyMatcher.group(1);
                // 如果策略没有正确结束，添加结束引号和大括号
                if (!lastPart.endsWith("\"")) {
                    result = result.replace(lastPart, lastPart + "\"");
                }
                // 如果数组没有正确结束，添加结束括号
                if (!result.contains("\"}]")) {
                    result = result + "}]}";
                }
            }
        }
        
        return result;
    }
    
    /**
     * 提取和修复预期成果字段
     */
    private JSONObject extractExpectedOutcomes(String json) {
        JSONObject outcomes = new JSONObject();
        try {
            // 1. 尝试从JSON对象中提取预期成果（常规格式）
            Pattern outcomesObjPattern = Pattern.compile(
                "\"expectedOutcomes\"\\s*:\\s*\\{([^\\}]*)\\}",
                Pattern.DOTALL
            );
            Matcher outcomesObjMatcher = outcomesObjPattern.matcher(json);
            if (outcomesObjMatcher.find()) {
                String outcomesContent = outcomesObjMatcher.group(1);
                try {
                    // 尝试解析为JSON对象
                    JSONObject parsedOutcomes = JSONUtil.parseObj("{" + outcomesContent + "}");
                    outcomes = parsedOutcomes;
                    
                    // 确保所有所需字段存在
                    if (!outcomes.containsKey("knowledge") || outcomes.getStr("knowledge").isEmpty()) {
                        extractKnowledgeOutcome(json, outcomes);
                    }
                    if (!outcomes.containsKey("skills") || outcomes.getStr("skills").isEmpty()) {
                        extractSkillsOutcome(json, outcomes);
                    }
                    if (!outcomes.containsKey("attitudes") || outcomes.getStr("attitudes").isEmpty()) {
                        extractAttitudesOutcome(json, outcomes);
                    }
                    
                    return outcomes;
        } catch (Exception e) {
                    log.debug("预期成果无法直接解析为对象：{}", e.getMessage());
                }
            }
            
            // 2. 尝试从数组格式的预期成果中提取
            Pattern outcomesArrayPattern = Pattern.compile(
                "\"expectedOutcomes\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL
            );
            Matcher outcomesArrayMatcher = outcomesArrayPattern.matcher(json);
            if (outcomesArrayMatcher.find()) {
                extractFromExpectedOutcomesArray(json, outcomes);
                return outcomes;
            }
            
            // 3. 单独提取各个预期成果字段
            extractKnowledgeOutcome(json, outcomes);
            extractSkillsOutcome(json, outcomes);
            extractAttitudesOutcome(json, outcomes);
            
        } catch (Exception e) {
            log.warn("提取预期成果失败: {}", e.getMessage());
        }
        
        // 如果仍然没有有效数据，提供默认值
        ensureDefaultOutcomes(outcomes);
        
        return outcomes;
    }
    
    /**
     * 从数组格式的expectedOutcomes中提取内容
     */
    private void extractFromExpectedOutcomesArray(String json, JSONObject outcomes) {
        try {
            // 尝试匹配具有knowledge字段的数组项
            Pattern knowledgePattern = Pattern.compile(
                "\\{\\s*\"knowledge\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher knowledgeMatcher = knowledgePattern.matcher(json);
            if (knowledgeMatcher.find()) {
                outcomes.set("knowledge", knowledgeMatcher.group(1));
            }
            
            // 尝试匹配具有skills字段的数组项
            Pattern skillsPattern = Pattern.compile(
                "\\{\\s*\"skills\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher skillsMatcher = skillsPattern.matcher(json);
            if (skillsMatcher.find()) {
                outcomes.set("skills", skillsMatcher.group(1));
            }
            
            // 尝试匹配具有attitudes字段的数组项
            Pattern attitudesPattern = Pattern.compile(
                "\\{\\s*\"attitudes\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher attitudesMatcher = attitudesPattern.matcher(json);
            if (attitudesMatcher.find()) {
                outcomes.set("attitudes", attitudesMatcher.group(1));
            }
        } catch (Exception e) {
            log.warn("从数组格式中提取预期成果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 提取知识预期成果
     */
    private void extractKnowledgeOutcome(String json, JSONObject outcomes) {
        try {
            Pattern knowledgePattern = Pattern.compile(
                "\"knowledge\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher knowledgeMatcher = knowledgePattern.matcher(json);
            if (knowledgeMatcher.find()) {
                outcomes.set("knowledge", knowledgeMatcher.group(1));
            }
        } catch (Exception e) {
            log.debug("提取知识预期成果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 提取能力预期成果
     */
    private void extractSkillsOutcome(String json, JSONObject outcomes) {
        try {
            Pattern skillsPattern = Pattern.compile(
                "\"skills\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher skillsMatcher = skillsPattern.matcher(json);
            if (skillsMatcher.find()) {
                outcomes.set("skills", skillsMatcher.group(1));
            } else {
                // 尝试从教学目标中提取能力内容
                Pattern objectivePattern = Pattern.compile(
                    "\"type\"\\s*:\\s*\"能力\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                    Pattern.DOTALL
                );
                Matcher objectiveMatcher = objectivePattern.matcher(json);
                if (objectiveMatcher.find()) {
                    outcomes.set("skills", objectiveMatcher.group(1));
                }
            }
        } catch (Exception e) {
            log.debug("提取能力预期成果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 提取态度预期成果
     */
    private void extractAttitudesOutcome(String json, JSONObject outcomes) {
        try {
            Pattern attitudesPattern = Pattern.compile(
                "\"attitudes\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher attitudesMatcher = attitudesPattern.matcher(json);
            if (attitudesMatcher.find()) {
                outcomes.set("attitudes", attitudesMatcher.group(1));
            } else {
                // 尝试从教学目标中提取情感内容
                Pattern objectivePattern = Pattern.compile(
                    "\"type\"\\s*:\\s*\"情感\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                    Pattern.DOTALL
                );
                Matcher objectiveMatcher = objectivePattern.matcher(json);
                if (objectiveMatcher.find()) {
                    outcomes.set("attitudes", objectiveMatcher.group(1));
                }
            }
        } catch (Exception e) {
            log.debug("提取态度预期成果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 确保预期成果包含所有必要的字段
     */
    private void ensureDefaultOutcomes(JSONObject outcomes) {
        if (!outcomes.containsKey("knowledge") || outcomes.getStr("knowledge").isEmpty()) {
            outcomes.set("knowledge", "学生能够系统地掌握导数的基本概念、定义、性质和运算法则，理解导数的几何意义，掌握基本初等函数的导数公式，能够熟练计算简单函数的导数。");
        }
        
        if (!outcomes.containsKey("skills") || outcomes.getStr("skills").isEmpty()) {
            outcomes.set("skills", "学生能够运用导数解决实际问题，具备分析函数性质、求解最值问题的能力，培养数学思维和逻辑推理能力，提高解决复杂问题的综合分析能力。");
        }
        
        if (!outcomes.containsKey("attitudes") || outcomes.getStr("attitudes").isEmpty()) {
            outcomes.set("attitudes", "学生能够保持积极的学习态度，具备良好的学习习惯，培养对数学的兴趣，发展团队合作意识，建立正确的数学观念，认识到数学在现实生活中的广泛应用。");
        }
    }

    /**
     * 清理和修复AI响应中的JSON
     */
    private String cleanJsonResponse(String response) {
        try {
            log.debug("开始处理AI响应...");
            // 移除可能存在的Markdown代码块标记
            String cleaned = response;
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "");
            
            // 尝试直接解析JSON，检查是否需要修复
            try {
                JSONUtil.parseObj(cleaned);
                log.debug("JSON解析成功，无需修复");
                return cleaned;
            } catch (Exception e) {
                log.debug("初步JSON解析失败: {}, 尝试修复...", e.getMessage());
                
                // 应用全面的修复逻辑
                String repaired = repairAIResponseJson(cleaned);
                
                // 验证修复后的JSON
                try {
                    JSONUtil.parseObj(repaired);
                    log.debug("JSON修复成功");
                    return repaired;
                } catch (Exception e2) {
                    log.warn("JSON修复后解析仍然失败: {}", e2.getMessage());
                    log.error("修复后无法解析的JSON内容: {}", repaired);
                    
                    // 作为最后手段，尝试提取那些能被提取的部分
                    // 提取出各个关键字段，构建一个尽可能完善的JSON对象
                    JSONObject fallbackJson = new JSONObject();
                    
                    // 提取知识分析部分
                    try {
                        Pattern knowledgeAnalysisPattern = Pattern.compile(
                            "\"knowledgeAnalysis\"\\s*:\\s*\\{([^\\}]*)\\}",
                            Pattern.DOTALL
                        );
                        Matcher knowledgeAnalysisMatcher = knowledgeAnalysisPattern.matcher(repaired);
                        if (knowledgeAnalysisMatcher.find()) {
                            String knowledgeAnalysisContent = knowledgeAnalysisMatcher.group(1);
                            fallbackJson.set("knowledgeAnalysis", "{" + knowledgeAnalysisContent + "}");
                        }
                    } catch (Exception ignored) {}
                    
                    // 提取教学目标部分
                    JSONArray objectives = extractTeachingObjectives(repaired);
                    if (objectives != null && !objectives.isEmpty()) {
                        fallbackJson.set("teachingObjectives", objectives);
                    }
                    
                    // 提取教学安排部分
                    JSONArray arrangement = extractTeachingArrangement(repaired);
                    if (arrangement != null && !arrangement.isEmpty()) {
                        fallbackJson.set("teachingArrangement", arrangement);
                    }
                    
                    // 提取预期成果部分
                    JSONObject outcomes = extractExpectedOutcomes(repaired);
                    if (outcomes != null && !outcomes.isEmpty()) {
                        fallbackJson.set("expectedOutcomes", outcomes);
                    }
                    
                    // 提取评价方法部分
                    JSONArray methods = extractEvaluationMethods(repaired);
                    if (methods != null && !methods.isEmpty()) {
                        fallbackJson.set("evaluationMethods", methods);
                    }
                    
                    return fallbackJson.toString();
                }
            }
        } catch (Exception e) {
            log.error("清理JSON响应失败: {}", e.getMessage());
            throw new RuntimeException("AI响应解析失败", e);
        }
    }

    /**
     * 尝试从JSON文本中提取指定字段并设置到JSON对象中
     */
    private void extractAndSetField(String json, String fieldName, JSONObject target) {
        try {
            Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\{[^}]*\\}|\\[[^\\]]*\\])", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                String fieldJson = matcher.group(1);
                // 移除多余的逗号
                fieldJson = fieldJson.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
                
                try {
                    if (fieldJson.startsWith("{")) {
                        JSONObject obj = JSONUtil.parseObj(fieldJson);
                        target.set(fieldName, obj);
                    } else if (fieldJson.startsWith("[")) {
                        JSONArray arr = JSONUtil.parseArray(fieldJson);
                        target.set(fieldName, arr);
                    }
                } catch (Exception e) {
                    log.warn("解析提取的字段 {} 失败: {}", fieldName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("提取字段 {} 失败: {}", fieldName, e.getMessage());
        }
    }
    
    /**
     * 修复括号不匹配的问题
     */
    private String fixBrackets(String json) {
        // 计算各种括号的数量
        int curlyOpen = 0, curlyClose = 0;
        int squareOpen = 0, squareClose = 0;
        
        for (char c : json.toCharArray()) {
            if (c == '{') curlyOpen++;
            else if (c == '}') curlyClose++;
            else if (c == '[') squareOpen++;
            else if (c == ']') squareClose++;
        }
        
        // 添加缺少的括号
        StringBuilder fixed = new StringBuilder(json);
        
        // 添加缺少的花括号
        while (curlyClose < curlyOpen) {
            fixed.append("}");
            curlyClose++;
        }
        
        // 添加缺少的方括号
        while (squareClose < squareOpen) {
            fixed.append("]");
            squareClose++;
        }
        
        return fixed.toString();
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
        prompt.append("你是一位专业的教育教学专家，请根据以下学生答题情况，生成一份详细的教学计划。\n\n");
        
        // 获取学科信息，确保包含教育阶段（如高中、初中）
        String subject = questionBank.getSubject();
        boolean hasEducationLevel = subject.contains("高中") || subject.contains("初中") || 
                                   subject.contains("小学") || subject.contains("大学");
        
        // 题库信息
        prompt.append("## 题库信息\n");
        prompt.append("- 题库标题: ").append(questionBank.getTitle()).append("\n");
        
        // 强调学科及其教育阶段
        if (hasEducationLevel) {
            prompt.append("- 学科: ").append(subject).append("（请在生成的教案中充分体现这是").append(subject).append("的教案）\n");
        } else {
            // 如果原始学科名称不包含教育阶段，尝试从题目标题中推断
            String title = questionBank.getTitle();
            if (title.contains("高中")) {
                prompt.append("- 学科: 高中").append(subject).append("（请在生成的教案中充分体现这是高中").append(subject).append("的教案）\n");
            } else if (title.contains("初中")) {
                prompt.append("- 学科: 初中").append(subject).append("（请在生成的教案中充分体现这是初中").append(subject).append("的教案）\n");
            } else if (title.contains("小学")) {
                prompt.append("- 学科: 小学").append(subject).append("（请在生成的教案中充分体现这是小学").append(subject).append("的教案）\n");
            } else {
                prompt.append("- 学科: ").append(subject).append("\n");
            }
        }
        
        prompt.append("- 总分: ").append(questionBank.getTotalScore()).append("\n");
        prompt.append("- 及格分: ").append(questionBank.getPassScore()).append("\n\n");
        
        // 学生答题情况
        prompt.append("## 学生答题情况\n");
        prompt.append("- 得分: ").append(userAnswerVO.getResultScore()).append("\n");
        prompt.append("- 评价: ").append(userAnswerVO.getResultName()).append("\n");
        if (userAnswerVO.getResultDesc() != null) {
            prompt.append("- 详细评价: ").append(userAnswerVO.getResultDesc()).append("\n");
        }
        if (weakKnowledgePoints != null && !weakKnowledgePoints.isEmpty()) {
            prompt.append("- 薄弱知识点: ").append(String.join(", ", weakKnowledgePoints)).append("\n");
        }
        prompt.append("\n");
        
        // 要求生成的教学计划格式
        prompt.append("## 教学计划需求\n");
        prompt.append("请生成一份详细的教学计划，包含以下内容（所有内容需要非常详细具体）：\n\n");
        
        // 知识分析
        prompt.append("### 1. 知识分析 (knowledgeAnalysis)\n");
        prompt.append("以JSON格式提供以下三部分分析：\n");
        prompt.append("- masteryLevel: 学生知识掌握情况的详细分析\n");
        prompt.append("- commonProblems: 学生普遍存在的学习问题，需要深入且具体\n");
        prompt.append("- errorAnalysis: 详细的错误分析及改进建议，需要围绕具体知识点展开\n\n");
        
        // 知识点
        prompt.append("### 2. 知识点 (knowledgePoints)\n");
        prompt.append("请提供3-5个关键知识点，这些知识点应该：\n");
        prompt.append("- 是简洁的技术术语或概念（例如：\"函数单调性\"、\"三角函数\"、\"立体几何\"），而非句子或短语\n");
        prompt.append("- 直接针对学生的薄弱环节和错误\n");
        prompt.append("- 每个知识点应该是2-4个字的专业术语，最多不超过5个字\n");
        prompt.append("- 知识点应该是专业的数学概念，而不是一般性描述\n");
        prompt.append("例如：[\"函数单调性\", \"三角函数\", \"立体几何\", \"概率计算\", \"数列求和\"]\n\n");
        
        // 教学目标
        prompt.append("### 3. 教学目标 (teachingObjectives)\n");
        prompt.append("按知识、能力、情感三个维度设定具体的教学目标，每个维度至少包含3个具体目标，需要非常详细：\n");
        prompt.append("```json\n");
        prompt.append("[\n");
        prompt.append("  {\"type\": \"知识\", \"content\": \"详细的知识目标内容，至少5点\"},\n");
        prompt.append("  {\"type\": \"能力\", \"content\": \"详细的能力目标内容，至少5点\"},\n");
        prompt.append("  {\"type\": \"情感\", \"content\": \"详细的情感目标内容，至少5点\"}\n");
        prompt.append("]\n");
        prompt.append("```\n\n");
        
        // 教学安排
        prompt.append("### 4. a教学安排 (teachingArrangement)\n");
        prompt.append("详细规划教学活动，包括导入、新知讲解、巩固练习和总结反馈四个环节，每个环节需要包含以下内容：\n");
        prompt.append("- stage: 教学阶段名称\n");
        prompt.append("- duration: 时间安排\n");
        prompt.append("- content: 详细的教学内容（需要极其详细，包含具体的例题、教学策略和技巧）\n");
        prompt.append("- methods: 具体的教学方法数组\n");
        prompt.append("- materials: 教学材料数组\n");
        prompt.append("- activities: 具体的教学活动数组\n\n");
        prompt.append("每个阶段的content内容应非常具体，包括要讲解的知识点和案例。例如，不要只说\"讲解函数的概念\"，而应该详细说明\"通过日常生活中的例子引入函数概念，例如温度随时间变化的关系，然后讲解函数f(x)=2x+3的图像特征，分析其中的参数意义...\"等\n\n");
        
        // 预期成果
        prompt.append("## 预期成果\n");
        prompt.append("请根据以上信息，详细描述教学后学生应该达到的预期成果，包括知识、能力和态度三个方面。输出应为JSON格式的数组，每个元素包含type和content两个字段。例如：\n");
        prompt.append("```json\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"type\": \"知识\",\n");
        prompt.append("    \"content\": \"学生能够准确理解并应用相关概念和原理...\"\n");
        prompt.append("  },\n");
        prompt.append("  {\n");
        prompt.append("    \"type\": \"能力\",\n");
        prompt.append("    \"content\": \"学生能够运用所学知识解决实际问题...\"\n");
        prompt.append("  },\n");
        prompt.append("  {\n");
        prompt.append("    \"type\": \"态度\",\n");
        prompt.append("    \"content\": \"学生对学习产生浓厚兴趣，具备积极的学习态度...\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        prompt.append("```\n\n");
        
        // 评价方法
        prompt.append("### 6. 评价方法 (evaluationMethods)\n");
        prompt.append("提供至少3种评价方法，包括过程性评价、形成性评价和总结性评价，每种评价方法需要包含：\n");
        prompt.append("- type: 评价类型\n");
        prompt.append("- description: 详细的评价描述\n");
        prompt.append("- methods: 具体的评价方法数组\n\n");
        
        // 输出格式要求
        prompt.append("## 输出格式\n");
        prompt.append("请按以下JSON格式输出完整的教学计划：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"knowledgePoints\": [\"知识点1\", \"知识点2\", \"知识点3\", \"知识点4\", \"知识点5\"],\n");
        prompt.append("  \"knowledgeAnalysis\": {\n");
        prompt.append("    \"masteryLevel\": \"学生知识掌握情况的详细分析\",\n");
        prompt.append("    \"commonProblems\": \"学生普遍存在的学习问题，需要深入且具体\",\n");
        prompt.append("    \"errorAnalysis\": \"详细的错误分析及改进建议，需要围绕具体知识点展开\"\n");
        prompt.append("  },\n");
        prompt.append("  \"teachingObjectives\": [\n");
        prompt.append("    {\"type\": \"知识\", \"content\": \"详细的知识目标内容，至少5点\"},\n");
        prompt.append("    {\"type\": \"能力\", \"content\": \"详细的能力目标内容，至少5点\"},\n");
        prompt.append("    {\"type\": \"情感\", \"content\": \"详细的情感目标内容，至少5点\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"teachingArrangement\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"stage\": \"导入阶段\",\n");
        prompt.append("      \"duration\": \"10-15分钟\",\n");
        prompt.append("      \"content\": \"极其详细的教学内容描述，包含具体案例和讲解方式\",\n");
        prompt.append("      \"methods\": [\"方法1\", \"方法2\", \"方法3\"],\n");
        prompt.append("      \"materials\": [\"材料1\", \"材料2\", \"材料3\"],\n");
        prompt.append("      \"activities\": [\"活动1\", \"活动2\", \"活动3\"]\n");
        prompt.append("    },\n");
        prompt.append("    // 其他阶段...\n");
        prompt.append("  ],\n");
        prompt.append("  \"expectedOutcomes\": {\n");
        prompt.append("    \"knowledge\": \"详细的知识获取成果，至少4点\",\n");
        prompt.append("    \"skills\": \"详细的技能提升成果，至少4点\",\n");
        prompt.append("    \"attitudes\": \"详细的态度变化成果，至少4点\"\n");
        prompt.append("  },\n");
        prompt.append("  \"evaluationMethods\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"过程性评价\",\n");
        prompt.append("      \"description\": \"详细的评价描述\",\n");
        prompt.append("      \"methods\": [\"方法1\", \"方法2\", \"方法3\"]\n");
        prompt.append("    },\n");
        prompt.append("    // 其他评价方法...\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("请确保所有内容都非常详细具体，特别是教学安排部分的content字段，应该包含具体的案例和教学内容，而不是泛泛而谈。");
        
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
        teachingPlan.setUserAnswerId(userAnswerVO.getId());
            
        // 直接使用题库中的原始学科，保留教育阶段信息（如"高中数学"）
        teachingPlan.setSubject(questionBank.getSubject());
        teachingPlan.setTitle(questionBank.getTitle()); // Set title from QuestionBank
        
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
            try {
                Object evalObj = jsonResponse.get("evaluationMethods");
                if (evalObj instanceof JSONArray) {
                    teachingPlan.setEvaluationMethods(evalObj.toString());
                } else {
                    log.warn("evaluationMethods格式异常，尝试提取");
                    JSONArray methods = extractEvaluationMethods(aiResponse);
                    teachingPlan.setEvaluationMethods(methods.toString());
                }
            } catch (Exception e) {
                log.warn("处理evaluationMethods失败: {}", e.getMessage());
                JSONArray methods = extractEvaluationMethods(aiResponse);
                teachingPlan.setEvaluationMethods(methods.toString());
            }
        } else {
            log.warn("评价方法字段(evaluationMethods)不存在，尝试从原始响应中提取");
            JSONArray methods = extractEvaluationMethods(aiResponse);
            teachingPlan.setEvaluationMethods(methods.toString());
        }
    
        return teachingPlan;
        } catch (Exception e) {
            log.error("解析教学计划失败，原始响应: {}", aiResponse, e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解析教学计划失败：" + e.getMessage());
        }
    }

    /**
     * 提取和修复评估方法字段
     */
    private JSONArray extractEvaluationMethods(String json) {
        JSONArray methods = new JSONArray();
        try {
            // 1. 尝试匹配评估方法数组
            Pattern methodsPattern = Pattern.compile(
                "\"evaluationMethods\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL
            );
            Matcher methodsMatcher = methodsPattern.matcher(json);
            if (methodsMatcher.find()) {
                String methodsContent = methodsMatcher.group(1);
                try {
                    // 尝试解析为JSON数组
                    String arrayJson = "[" + methodsContent + "]";
                    arrayJson = fixMissingCommas(arrayJson);  // 修复可能缺失的逗号
                    JSONArray parsedMethods = JSONUtil.parseArray(arrayJson);
                    if (parsedMethods != null && !parsedMethods.isEmpty()) {
                        return parsedMethods;
                    }
                } catch (Exception e) {
                    log.debug("评估方法数组无法直接解析: {}", e.getMessage());
                }
                
                // 尝试单独提取每个评估方法对象
                extractMethodsItems(methodsContent, methods);
            }
            
            // 2. 如果上面方法失败，尝试直接匹配类型和描述
            if (methods.isEmpty()) {
                extractMethodsByTypeAndDesc(json, methods);
            }
            
            // 3. 检查是否从JSON中发现任何评估方法信息
            if (methods.isEmpty()) {
                // 尝试从常见关键词检测评估方法
                extractMethodsFromKeywords(json, methods);
            }
        } catch (Exception e) {
            log.warn("提取评估方法失败: {}", e.getMessage());
        }
        
        // 如果没有找到任何评估方法，添加默认值
        if (methods.isEmpty()) {
            addDefaultEvaluationMethods(methods);
        }
        
        return methods;
    }
    
    /**
     * 从评估方法内容中提取每个评估方法项
     */
    private void extractMethodsItems(String methodsContent, JSONArray methods) {
        try {
            // 匹配每个评估方法对象的模式
            Pattern itemPattern = Pattern.compile(
                "\\{([^\\{\\}]+)\\}",
                Pattern.DOTALL
            );
            Matcher itemMatcher = itemPattern.matcher(methodsContent);
            
            while (itemMatcher.find()) {
                String itemContent = itemMatcher.group(1);
                try {
                    JSONObject methodObj = new JSONObject();
                    
                    // 提取类型
                    Pattern typePattern = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher typeMatcher = typePattern.matcher(itemContent);
                    if (typeMatcher.find()) {
                        methodObj.set("type", typeMatcher.group(1));
                    }
                    
                    // 提取描述
                    Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher descMatcher = descPattern.matcher(itemContent);
                    if (descMatcher.find()) {
                        methodObj.set("description", descMatcher.group(1));
                    }
                    
                    // 提取方法数组
                    Pattern methodsArrayPattern = Pattern.compile("\"methods\"\\s*:\\s*\\[(.*?)\\]");
                    Matcher methodsArrayMatcher = methodsArrayPattern.matcher(itemContent);
                    if (methodsArrayMatcher.find()) {
                        String methodsArrayContent = methodsArrayMatcher.group(1);
                        JSONArray methodsArray = new JSONArray();
                        
                        // 提取方法数组中的每个项目
                        Pattern methodItemPattern = Pattern.compile("\"([^\"]+)\"");
                        Matcher methodItemMatcher = methodItemPattern.matcher(methodsArrayContent);
                        while (methodItemMatcher.find()) {
                            methodsArray.add(methodItemMatcher.group(1));
                        }
                        
                        if (!methodsArray.isEmpty()) {
                            methodObj.set("methods", methodsArray);
                        }
                    }
                    
                    // 确保对象至少有类型和描述
                    if (methodObj.containsKey("type") || methodObj.containsKey("description")) {
                        // 设置默认值为有缺失的字段
                        if (!methodObj.containsKey("type")) {
                            methodObj.set("type", "评价方法");
                        }
                        if (!methodObj.containsKey("description")) {
                            methodObj.set("description", "未提供描述");
                        }
                        if (!methodObj.containsKey("methods")) {
                            JSONArray defaultMethods = new JSONArray();
                            defaultMethods.add("观察");
                            defaultMethods.add("测试");
                            methodObj.set("methods", defaultMethods);
                        }
                        
                        methods.add(methodObj);
                    }
                } catch (Exception e) {
                    log.debug("提取单个评估方法项失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("提取评估方法项失败: {}", e.getMessage());
        }
    }
    
    /**
     * 通过类型和描述提取评估方法
     */
    private void extractMethodsByTypeAndDesc(String json, JSONArray methods) {
        try {
            // 匹配评价类型和描述的模式
            Pattern typeDescPattern = Pattern.compile(
                "\"type\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.DOTALL
            );
            Matcher typeDescMatcher = typeDescPattern.matcher(json);
            
            while (typeDescMatcher.find()) {
                JSONObject methodObj = new JSONObject();
                methodObj.set("type", typeDescMatcher.group(1));
                methodObj.set("description", typeDescMatcher.group(2));
                
                // 添加默认方法
                JSONArray defaultMethods = new JSONArray();
                defaultMethods.add("观察");
                defaultMethods.add("测试");
                methodObj.set("methods", defaultMethods);
                
                methods.add(methodObj);
            }
        } catch (Exception e) {
            log.warn("通过类型和描述提取评估方法失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从常见关键词中提取评估方法
     */
    private void extractMethodsFromKeywords(String json, JSONArray methods) {
        try {
            // 常见评估方法类型
            String[] evalTypes = {"过程性评价", "形成性评价", "总结性评价", "诊断性评价", "自我评价", "同伴评价"};
            
            for (String type : evalTypes) {
                if (json.contains(type)) {
                    // 尝试提取此类型周围的描述
                    Pattern descPattern = Pattern.compile(
                        type + "([^。，；：]+)[。，；：]",
                        Pattern.DOTALL
                    );
                    Matcher descMatcher = descPattern.matcher(json);
                    
                    JSONObject methodObj = new JSONObject();
                    methodObj.set("type", type);
                    
                    if (descMatcher.find()) {
                        methodObj.set("description", descMatcher.group(1).trim());
                    } else {
                        methodObj.set("description", "使用" + type + "评估学生学习情况");
                    }
                    
                    // 添加默认方法
                    JSONArray defaultMethods = new JSONArray();
                    if (type.equals("过程性评价")) {
                        defaultMethods.add("课堂观察");
                        defaultMethods.add("提问");
                    } else if (type.equals("形成性评价")) {
                        defaultMethods.add("作业");
                        defaultMethods.add("小测验");
                    } else if (type.equals("总结性评价")) {
                        defaultMethods.add("单元测试");
                        defaultMethods.add("期末考试");
                    } else {
                        defaultMethods.add("观察");
                        defaultMethods.add("测试");
                    }
                    methodObj.set("methods", defaultMethods);
                    
                    methods.add(methodObj);
                }
            }
        } catch (Exception e) {
            log.warn("从关键词提取评估方法失败: {}", e.getMessage());
        }
    }
    
    /**
     * 添加默认评估方法
     */
    private void addDefaultEvaluationMethods(JSONArray methods) {
        // 过程性评价
        JSONObject processEval = new JSONObject();
        processEval.set("type", "过程性评价");
        processEval.set("description", "通过课堂观察、学生参与情况和表现来进行评价");
        JSONArray processMethods = new JSONArray();
        processMethods.add("课堂观察");
        processMethods.add("学生参与度");
        processMethods.add("课堂提问");
        processEval.set("methods", processMethods);
        methods.add(processEval);
        
        // 形成性评价
        JSONObject formativeEval = new JSONObject();
        formativeEval.set("type", "形成性评价");
        formativeEval.set("description", "通过作业、小测验等方式对学习过程进行评价");
        JSONArray formativeMethods = new JSONArray();
        formativeMethods.add("作业");
        formativeMethods.add("小测验");
        formativeMethods.add("课堂练习");
        formativeEval.set("methods", formativeMethods);
        methods.add(formativeEval);
        
        // 总结性评价
        JSONObject summativeEval = new JSONObject();
        summativeEval.set("type", "总结性评价");
        summativeEval.set("description", "通过期末考试、综合项目等方式对学习成果进行全面评价");
        JSONArray summativeMethods = new JSONArray();
        summativeMethods.add("期末考试");
        summativeMethods.add("综合项目");
        summativeMethods.add("学习档案");
        summativeEval.set("methods", summativeMethods);
        methods.add(summativeEval);
    }
    
    /**
     * 提取和修正教学安排字段
     */
    private JSONArray extractTeachingArrangement(String json) {
        JSONArray arrangement = new JSONArray();
        
        try {
            // 检查是否有导入阶段
            if (json.contains("\"stage\"\\s*:\\s*\"导入阶段\"") || json.contains("\"stage\":\"导入阶段\"")) {
                // 尝试提取完整的教学安排数组
                Pattern arrPattern = Pattern.compile(
                    "\"teachingArrangement\"\\s*:\\s*\\[(.*?)\\]",
                    Pattern.DOTALL
                );
                Matcher arrMatcher = arrPattern.matcher(json);
                if (arrMatcher.find()) {
                    String arrContent = arrMatcher.group(1);
                    // 尝试解析为数组
                    try {
                        JSONArray parsed = JSONUtil.parseArray("[" + arrContent + "]");
                        return parsed;
                    } catch (Exception e) {
                        log.debug("教学安排数组解析失败，尝试提取单个阶段");
                    }
                }
                
                // 尝试提取各个阶段
                String[] stages = {"导入阶段", "新知讲解阶段", "巩固练习阶段", "总结反馈阶段"};
                String[] durations = {"10-15分钟", "20-25分钟", "15-20分钟", "10-15分钟"};
                
                for (int i = 0; i < stages.length; i++) {
                    String stage = stages[i];
                    Pattern stagePattern = Pattern.compile(
                        "\"stage\"\\s*:\\s*\"" + stage + "\"\\s*,\\s*\"duration\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                        Pattern.DOTALL
                    );
                    Matcher stageMatcher = stagePattern.matcher(json);
                    if (stageMatcher.find()) {
                        JSONObject stageObj = new JSONObject();
                        stageObj.set("stage", stage);
                        stageObj.set("duration", stageMatcher.group(1));
                        stageObj.set("content", stageMatcher.group(2));
                        
                        // 尝试提取methods, materials和activities
                        Pattern methodsPattern = Pattern.compile(
                            "\"methods\"\\s*:\\s*\\[(.*?)\\]",
                            Pattern.DOTALL
                        );
                        Matcher methodsMatcher = methodsPattern.matcher(json.substring(stageMatcher.start()));
                        if (methodsMatcher.find()) {
                            String methodsStr = methodsMatcher.group(1).trim();
                            if (!methodsStr.isEmpty()) {
                                JSONArray methods = new JSONArray();
                                String[] methodItems = methodsStr.split(",");
                                for (String item : methodItems) {
                                    methods.add(item.trim().replace("\"", ""));
                                }
                                stageObj.set("methods", methods);
                            } else {
                                stageObj.set("methods", new JSONArray());
                            }
                        } else {
                            stageObj.set("methods", new JSONArray());
                        }
                        
                        Pattern materialsPattern = Pattern.compile(
                            "\"materials\"\\s*:\\s*\\[(.*?)\\]",
                            Pattern.DOTALL
                        );
                        Matcher materialsMatcher = materialsPattern.matcher(json.substring(stageMatcher.start()));
                        if (materialsMatcher.find()) {
                            String materialsStr = materialsMatcher.group(1).trim();
                            if (!materialsStr.isEmpty()) {
                                JSONArray materials = new JSONArray();
                                String[] materialItems = materialsStr.split(",");
                                for (String item : materialItems) {
                                    materials.add(item.trim().replace("\"", ""));
                                }
                                stageObj.set("materials", materials);
                            } else {
                                stageObj.set("materials", new JSONArray());
                            }
                        } else {
                            stageObj.set("materials", new JSONArray());
                        }
                        
                        Pattern activitiesPattern = Pattern.compile(
                            "\"activities\"\\s*:\\s*\\[(.*?)\\]",
                            Pattern.DOTALL
                        );
                        Matcher activitiesMatcher = activitiesPattern.matcher(json.substring(stageMatcher.start()));
                        if (activitiesMatcher.find()) {
                            String activitiesStr = activitiesMatcher.group(1).trim();
                            if (!activitiesStr.isEmpty()) {
                                JSONArray activities = new JSONArray();
                                String[] activityItems = activitiesStr.split(",");
                                for (String item : activityItems) {
                                    activities.add(item.trim().replace("\"", ""));
                                }
                                stageObj.set("activities", activities);
                            } else {
                                stageObj.set("activities", new JSONArray());
                            }
                        } else {
                            stageObj.set("activities", new JSONArray());
                        }
                        
                        arrangement.add(stageObj);
                    }
                }
                
                if (!arrangement.isEmpty()) {
                    return arrangement;
                }
            }
            
            // 如果上述提取失败，尝试从示例提取
            if (json.contains("导入阶段") && json.contains("新知讲解阶段")) {
                JSONObject stage1 = new JSONObject();
                stage1.set("stage", "导入阶段");
                stage1.set("duration", "10-15分钟");
                stage1.set("content", "通过复习函数的基本概念和性质，引入新课内容。利用实际生活中的例子，激发学生的学习兴趣。");
                JSONArray methods1 = new JSONArray();
                methods1.add("复习旧知识");
                methods1.add("引入新概念");
                methods1.add("设置问题情境");
                stage1.set("methods", methods1);

                JSONArray materials1 = new JSONArray();
                materials1.add("黑板和粉笔");
                materials1.add("图表和图像");
                materials1.add("多媒体课件");
                stage1.set("materials", materials1);

                JSONArray activities1 = new JSONArray();
                activities1.add("教师提问");
                activities1.add("学生回答");
                activities1.add("小组讨论");
                stage1.set("activities", activities1);
                arrangement.add(stage1);
                
                JSONObject stage2 = new JSONObject();
                stage2.set("stage", "新知讲解阶段");
                stage2.set("duration", "20-25分钟");
                stage2.set("content", "详细讲解函数的概念、性质和图像，通过具体的例子和推导过程帮助学生理解。");
                JSONArray methods2 = new JSONArray();
                methods2.add("讲授法");
                methods2.add("案例分析");
                methods2.add("互动讨论");
                stage2.set("methods", methods2);

                JSONArray materials2 = new JSONArray();
                materials2.add("教材");
                materials2.add("函数图像");
                materials2.add("多媒体课件");
                stage2.set("materials", materials2);

                JSONArray activities2 = new JSONArray();
                activities2.add("教师讲解");
                activities2.add("学生笔记");
                activities2.add("分组讨论");
                stage2.set("activities", activities2);
                arrangement.add(stage2);
                
                JSONObject stage3 = new JSONObject();
                stage3.set("stage", "巩固练习阶段");
                stage3.set("duration", "15-20分钟");
                stage3.set("content", "通过基础练习、提高练习和拓展练习，帮助学生巩固所学知识。");
                JSONArray methods3 = new JSONArray();
                methods3.add("练习法");
                methods3.add("分组合作");
                methods3.add("个别辅导");
                stage3.set("methods", methods3);

                JSONArray materials3 = new JSONArray();
                materials3.add("练习题");
                materials3.add("计算器");
                materials3.add("辅助工具");
                stage3.set("materials", materials3);

                JSONArray activities3 = new JSONArray();
                activities3.add("学生独立练习");
                activities3.add("小组合作");
                activities3.add("教师辅导");
                stage3.set("activities", activities3);
                arrangement.add(stage3);
                
                JSONObject stage4 = new JSONObject();
                stage4.set("stage", "总结反馈阶段");
                stage4.set("duration", "10-15分钟");
                stage4.set("content", "引导学生总结本节课的知识要点和解题方法，进行知识反馈和评价。");
                JSONArray methods4 = new JSONArray();
                methods4.add("总结法");
                methods4.add("讨论法");
                methods4.add("评价法");
                stage4.set("methods", methods4);

                JSONArray materials4 = new JSONArray();
                materials4.add("黑板");
                materials4.add("总结提纲");
                stage4.set("materials", materials4);

                JSONArray activities4 = new JSONArray();
                activities4.add("学生总结");
                activities4.add("教师点评");
                activities4.add("反馈交流");
                stage4.set("activities", activities4);
                arrangement.add(stage4);
                
                return arrangement;
            }
        } catch (Exception e) {
            log.warn("提取教学安排字段失败: {}", e.getMessage());
        }
        
        // 如果所有提取方法都失败，提供默认值
        if (arrangement.isEmpty()) {
            JSONObject defaultStage = new JSONObject();
            defaultStage.set("stage", "导入阶段");
            defaultStage.set("duration", "10-15分钟");
            defaultStage.set("content", "复习旧知识，引入新课题");
            JSONArray defaultMethods = new JSONArray();
            defaultMethods.add("提问法");
            defaultMethods.add("讨论法");
            defaultStage.set("methods", defaultMethods);

            JSONArray defaultMaterials = new JSONArray();
            defaultMaterials.add("黑板");
            defaultMaterials.add("多媒体");
            defaultStage.set("materials", defaultMaterials);

            JSONArray defaultActivities = new JSONArray();
            defaultActivities.add("教师提问");
            defaultActivities.add("学生讨论");
            defaultStage.set("activities", defaultActivities);
            arrangement.add(defaultStage);
            
            JSONObject defaultStage2 = new JSONObject();
            defaultStage2.set("stage", "新知讲解阶段");
            defaultStage2.set("duration", "20-25分钟");
            defaultStage2.set("content", "讲解新知识点，分析例题");
            JSONArray defaultMethods2 = new JSONArray();
            defaultMethods2.add("讲授法");
            defaultMethods2.add("案例分析法");
            defaultStage2.set("methods", defaultMethods2);

            JSONArray defaultMaterials2 = new JSONArray();
            defaultMaterials2.add("教材");
            defaultMaterials2.add("课件");
            defaultStage2.set("materials", defaultMaterials2);

            JSONArray defaultActivities2 = new JSONArray();
            defaultActivities2.add("教师讲解");
            defaultActivities2.add("学生记笔记");
            defaultStage2.set("activities", defaultActivities2);
            arrangement.add(defaultStage2);
        }
        
        return arrangement;
    }

    /**
     * 提取和修复教学目标字段
     */
    private JSONArray extractTeachingObjectives(String json) {
        JSONArray objectives = new JSONArray();
        
        try {
            // 尝试从JSON中提取教学目标数组
            Pattern objectivesPattern = Pattern.compile(
                "\"teachingObjectives\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL
            );
            Matcher objectivesMatcher = objectivesPattern.matcher(json);
            
            if (objectivesMatcher.find()) {
                String objectivesContent = objectivesMatcher.group(1);
                objectivesContent = "{\"items\":[" + objectivesContent + "]}";
                
                try {
                    JSONObject parsed = JSONUtil.parseObj(objectivesContent);
                    JSONArray items = parsed.getJSONArray("items");
                    if (items != null && !items.isEmpty()) {
                        objectives = items;
                    }
                } catch (Exception e) {
                    log.debug("无法直接解析教学目标数组: {}", e.getMessage());
                }
            }
            
            // 如果数组没有内容，尝试单独提取目标
            if (objectives.isEmpty()) {
                extractIndividualObjectives(json, objectives);
            }
            
            // 确保有三种类型的目标
            ensureObjectiveTypes(objectives);
            
        } catch (Exception e) {
            log.warn("提取教学目标失败: {}", e.getMessage());
        }
        
        // 如果仍然没有目标，提供默认目标
        if (objectives.isEmpty()) {
            addDefaultObjectives(objectives);
        }
        
        return objectives;
    }
    
    /**
     * 从内容中单独提取每种教学目标
     */
    private void extractIndividualObjectives(String json, JSONArray objectives) {
        // 提取知识类目标
        if (!hasObjectiveOfType(objectives, "知识")) {
            Pattern knowledgePattern = Pattern.compile(
                "\"type\"\\s*:\\s*\"知识\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher knowledgeMatcher = knowledgePattern.matcher(json);
            if (knowledgeMatcher.find()) {
                JSONObject objective = new JSONObject();
                objective.set("type", "知识");
                objective.set("content", knowledgeMatcher.group(1));
                objectives.add(objective);
            }
        }
        
        // 提取能力类目标
        if (!hasObjectiveOfType(objectives, "能力")) {
            Pattern abilityPattern = Pattern.compile(
                "\"type\"\\s*:\\s*\"能力\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher abilityMatcher = abilityPattern.matcher(json);
            if (abilityMatcher.find()) {
                JSONObject objective = new JSONObject();
                objective.set("type", "能力");
                objective.set("content", abilityMatcher.group(1));
                objectives.add(objective);
            }
        }
        
        // 提取情感类目标
        if (!hasObjectiveOfType(objectives, "情感")) {
            Pattern emotionalPattern = Pattern.compile(
                "\"type\"\\s*:\\s*\"情感\"\\s*,\\s*\"content\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.DOTALL
            );
            Matcher emotionalMatcher = emotionalPattern.matcher(json);
            if (emotionalMatcher.find()) {
                JSONObject objective = new JSONObject();
                objective.set("type", "情感");
                objective.set("content", emotionalMatcher.group(1));
                objectives.add(objective);
            }
        }
    }
    
    /**
     * 检查是否已经有特定类型的教学目标
     */
    private boolean hasObjectiveOfType(JSONArray objectives, String type) {
        for (int i = 0; i < objectives.size(); i++) {
            JSONObject objective = objectives.getJSONObject(i);
            if (type.equals(objective.getStr("type"))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 确保教学目标包含所有必要的类型
     */
    private void ensureObjectiveTypes(JSONArray objectives) {
        // 检查并添加知识类目标
        if (!hasObjectiveOfType(objectives, "知识")) {
            JSONObject knowledgeObj = new JSONObject();
            knowledgeObj.set("type", "知识");
            knowledgeObj.set("content", "理解导数的概念和几何意义，掌握导数的计算方法和应用，能够熟练运用导数分析函数的性质。");
            objectives.add(knowledgeObj);
        }
        
        // 检查并添加能力类目标
        if (!hasObjectiveOfType(objectives, "能力")) {
            JSONObject abilityObj = new JSONObject();
            abilityObj.set("type", "能力");
            abilityObj.set("content", "培养逻辑思维和运算能力，提高分析问题和解决问题的能力，增强数学应用意识和创新思维。");
            objectives.add(abilityObj);
        }
        
        // 检查并添加情感类目标
        if (!hasObjectiveOfType(objectives, "情感")) {
            JSONObject emotionalObj = new JSONObject();
            emotionalObj.set("type", "情感");
            emotionalObj.set("content", "培养学生的数学学习兴趣，建立数学自信心，形成积极的学习态度和良好的学习习惯。");
            objectives.add(emotionalObj);
        }
    }
    
    /**
     * 添加默认的教学目标
     */
    private void addDefaultObjectives(JSONArray objectives) {
        // 添加知识类目标
        JSONObject knowledgeObj = new JSONObject();
        knowledgeObj.set("type", "知识");
        knowledgeObj.set("content", "理解导数的概念和几何意义，掌握导数的计算方法和应用，能够熟练运用导数分析函数的性质。");
        objectives.add(knowledgeObj);
        
        // 添加能力类目标
        JSONObject abilityObj = new JSONObject();
        abilityObj.set("type", "能力");
        abilityObj.set("content", "培养逻辑思维和运算能力，提高分析问题和解决问题的能力，增强数学应用意识和创新思维。");
        objectives.add(abilityObj);
        
        // 添加情感类目标
        JSONObject emotionalObj = new JSONObject();
        emotionalObj.set("type", "情感");
        emotionalObj.set("content", "培养学生的数学学习兴趣，建立数学自信心，形成积极的学习态度和良好的学习习惯。");
        objectives.add(emotionalObj);
    }

    private void generateTeachingPlan(String cleanedResponse, TeachingPlan teachingPlan) {
        log.info("开始解析AI响应并生成教学计划");
        try {
            JSONObject responseJson = JSONUtil.parseObj(cleanedResponse);
            
            // 保留原始学科名称，包含教育阶段（如"高中数学"、"初中语文"等）
            // 不要修改原始的学科名称，以保留教育阶段信息
            
            // 处理知识点 - 仅使用AI生成的知识点，不使用默认数据
            List<String> knowledgePoints = new ArrayList<>();
            if (responseJson.containsKey("knowledgePoints")) {
                Object kpObj = responseJson.get("knowledgePoints");
                if (kpObj instanceof JSONArray) {
                    knowledgePoints = ((JSONArray) kpObj).toList(String.class);
                    log.info("从AI响应中直接提取到{}个知识点", knowledgePoints.size());
                } else if (kpObj instanceof String) {
                    knowledgePoints.add(kpObj.toString());
                    log.info("从AI响应中提取到1个知识点（字符串形式）");
                }
            } 
            
            // 如果知识点为空，尝试从知识分析中提取知识点
            if (knowledgePoints.isEmpty() && responseJson.containsKey("knowledgeAnalysis")) {
                JSONObject analysis = responseJson.getJSONObject("knowledgeAnalysis");
                List<String> extractedPoints = extractKnowledgePointsFromAnalysis(analysis);
                if (!extractedPoints.isEmpty()) {
                    knowledgePoints.addAll(extractedPoints);
                    log.info("从知识分析中提取到{}个知识点", extractedPoints.size());
                }
            }
            
            // 设置知识点 - 即使列表为空也直接设置
            teachingPlan.setKnowledgePoints(knowledgePoints);
            
            // 处理知识分析 - 使用增强的修复方法处理恶劣格式
            if (responseJson.containsKey("knowledgeAnalysis")) {
                Object kaObj = responseJson.get("knowledgeAnalysis");
                if (kaObj instanceof JSONObject) {
                    teachingPlan.setKnowledgeAnalysis(kaObj);
                } else if (kaObj instanceof String) {
                    String kaStr = (String) kaObj;
                    // 使用增强的修复方法处理恶劣格式
                    JSONObject knowledgeAnalysisObj = fixMalformedKnowledgeAnalysis(kaStr);
                    teachingPlan.setKnowledgeAnalysis(knowledgeAnalysisObj);
                }
            } else {
                log.warn("知识分析字段(knowledgeAnalysis)不存在");
                JSONObject defaultAnalysis = new JSONObject();
                defaultAnalysis.set("masteryLevel", "知识掌握情况未分析");
                defaultAnalysis.set("commonProblems", "未发现普遍问题");
                defaultAnalysis.set("errorAnalysis", "未进行错误分析");
                teachingPlan.setKnowledgeAnalysis(defaultAnalysis);
            }
            
            // 其余处理保持不变...
            // 处理教学目标
            if (responseJson.containsKey("teachingObjectives")) {
                teachingPlan.setTeachingObjectives(responseJson.get("teachingObjectives"));
            } else {
                log.warn("教学目标字段(teachingObjectives)不存在，尝试从cleanedResponse中提取");
                JSONArray objectives = extractTeachingObjectives(cleanedResponse);
                if (objectives != null && !objectives.isEmpty()) {
                    teachingPlan.setTeachingObjectives(objectives);
                } else {
                    log.warn("无法提取教学目标，使用默认值");
                    JSONArray defaultObjectives = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.set("type", "知识");
                    obj.set("content", "教学目标未生成");
                    defaultObjectives.add(obj);
                    teachingPlan.setTeachingObjectives(defaultObjectives);
                }
            }
            
            // 处理教学安排
            if (responseJson.containsKey("teachingArrangement")) {
                teachingPlan.setTeachingArrangement(responseJson.get("teachingArrangement"));
            } else {
                log.warn("教学安排字段(teachingArrangement)不存在，尝试从cleanedResponse中提取");
                JSONArray arrangement = extractTeachingArrangement(cleanedResponse);
                if (arrangement != null && !arrangement.isEmpty()) {
                    teachingPlan.setTeachingArrangement(arrangement);
                } else {
                    log.warn("无法提取教学安排，使用默认值");
                    JSONArray defaultArrangement = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.set("stage", "导入");
                    obj.set("duration", "10分钟");
                    obj.set("content", "教学安排未生成");
                    JSONArray methodsArray = new JSONArray();
                    methodsArray.add("讲解");
                    obj.set("methods", methodsArray);

                    JSONArray materialsArray = new JSONArray();
                    materialsArray.add("无");
                    obj.set("materials", materialsArray);

                    JSONArray activitiesArray = new JSONArray();
                    activitiesArray.add("无");
                    obj.set("activities", activitiesArray);
                    defaultArrangement.add(obj);
                    teachingPlan.setTeachingArrangement(defaultArrangement);
                }
            }
            
            // 处理预期成果
            JSONObject expectedOutcomes = new JSONObject();
            if (responseJson.containsKey("expectedOutcomes")) {
                Object outcomesObj = responseJson.get("expectedOutcomes");
                if (outcomesObj instanceof JSONArray) {
                    // 如果是数组格式，将其转换为对象格式
                    JSONArray outcomesArray = (JSONArray) outcomesObj;
                    log.warn("预期成果格式为数组，尝试转换为对象格式");
                    
                    for (int i = 0; i < outcomesArray.size(); i++) {
                        JSONObject outcome = outcomesArray.getJSONObject(i);
                        if (outcome.containsKey("type") && outcome.containsKey("content")) {
                            String type = outcome.getStr("type");
                            String content = outcome.getStr("content");
                            
                            // 根据类型设置不同字段
                            if ("知识".equals(type)) {
                                expectedOutcomes.put("knowledge", content);
                            } else if ("技能".equals(type) || "能力".equals(type)) {
                                expectedOutcomes.put("skills", content);
                            } else if ("态度".equals(type) || "情感".equals(type)) {
                                expectedOutcomes.put("attitudes", content);
                            }
                        }
                    }
                } else if (outcomesObj instanceof JSONObject) {
                    // 如果已经是对象格式，直接使用
                    expectedOutcomes = (JSONObject) outcomesObj;
                }
            }
            
            // 确保所有预期成果字段都有值
            if (!expectedOutcomes.containsKey("knowledge")) {
                expectedOutcomes.put("knowledge", "知识预期成果未生成");
            }
            if (!expectedOutcomes.containsKey("skills")) {
                expectedOutcomes.put("skills", "能力预期成果未生成");
            }
            if (!expectedOutcomes.containsKey("attitudes")) {
                expectedOutcomes.put("attitudes", "态度预期成果未生成");
            }
            
            teachingPlan.setExpectedOutcomes(expectedOutcomes);
            
            // 处理评价方法
            if (responseJson.containsKey("evaluationMethods")) {
                try {
                    Object evalObj = responseJson.get("evaluationMethods");
                    if (evalObj instanceof JSONArray) {
                        teachingPlan.setEvaluationMethods(evalObj);
                    } else {
                        log.warn("evaluationMethods格式异常，尝试提取");
                        JSONArray methods = extractEvaluationMethods(cleanedResponse);
                        teachingPlan.setEvaluationMethods(methods);
                    }
                } catch (Exception e) {
                    log.warn("处理evaluationMethods失败: {}", e.getMessage());
                    JSONArray methods = extractEvaluationMethods(cleanedResponse);
                    teachingPlan.setEvaluationMethods(methods);
                }
            } else {
                log.warn("评价方法字段(evaluationMethods)不存在，尝试从cleanedResponse中提取");
                JSONArray methods = extractEvaluationMethods(cleanedResponse);
                teachingPlan.setEvaluationMethods(methods);
            }
            
            log.info("教学计划生成完成");
            
        } catch (Exception e) {
            log.error("生成教学计划失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成教学计划失败", e);
        }
    }

    /**
     * 全面修复AI响应JSON
     * 该方法整合了所有修复逻辑，确保JSON可以被正确解析
     */
    private String repairAIResponseJson(String json) {
        String repaired = json;
        
        // 先去除潜在的Markdown代码块标记
        repaired = repaired.replaceAll("```json", "").replaceAll("```", "");
        
        // 修复缺失的逗号
        repaired = fixMissingCommas(repaired);
        
        // 修复特定的AI响应错误
        repaired = fixSpecificAIResponseErrors(repaired);
        
        // 修复教学安排部分
        repaired = manuallyFixTeachingArrangement(repaired);
        
        // 修复被截断的JSON
        repaired = fixTruncatedJson(repaired);
        
        // 修复JSON括号匹配问题
        repaired = fixBrackets(repaired);
        
        // 检查并修复数组结尾
        repaired = fixArrayEndings(repaired);
        
        // 移除任何额外的JSON数组/对象结束标记
        repaired = removeExtraClosings(repaired);
        
        return repaired;
    }
    
    /**
     * 修复数组结尾问题
     */
    private String fixArrayEndings(String json) {
        // 检查和修复evaluationMethods结尾
        if (json.contains("\"evaluationMethods\"")) {
            // 查找evaluationMethods数组的结束位置
            Pattern evalArrayPattern = Pattern.compile(
                "\"evaluationMethods\"\\s*:\\s*\\[(.*?)\\]\\s*,?",
                Pattern.DOTALL
            );
            Matcher evalArrayMatcher = evalArrayPattern.matcher(json);
            
            if (!evalArrayMatcher.find()) {
                // 如果没有找到完整的闭合数组，可能是缺少结束括号
                Pattern openArrayPattern = Pattern.compile(
                    "\"evaluationMethods\"\\s*:\\s*\\[(.+?)(?=\\}\\s*\\}|$)",
                    Pattern.DOTALL
                );
                Matcher openArrayMatcher = openArrayPattern.matcher(json);
                
                if (openArrayMatcher.find()) {
                    String content = openArrayMatcher.group(1);
                    // 计算缺少的闭括号数量
                    int openBrackets = countChar(content, '[');
                    int closeBrackets = countChar(content, ']');
                    int missingCloseBrackets = openBrackets - closeBrackets;
                    
                    if (missingCloseBrackets > 0) {
                        // 找到evaluationMethods的位置
                        int evalStart = json.indexOf("\"evaluationMethods\"");
                        int contentEnd = evalStart + openArrayMatcher.end(1);
                        
                        // 插入缺少的闭括号
                        StringBuilder builder = new StringBuilder(json);
                        for (int i = 0; i < missingCloseBrackets; i++) {
                            builder.insert(contentEnd, ']');
                        }
                        
                        json = builder.toString();
                    }
                }
            }
        }
        
        return json;
    }
    
    /**
     * 移除额外的JSON闭合标记
     */
    private String removeExtraClosings(String json) {
        // 检查是否有额外的JSON结束标记
        String trimmed = json.trim();
        
        // 移除额外的结束括号/方括号
        while (trimmed.endsWith("}}") || trimmed.endsWith("]]") || 
               trimmed.endsWith("}]") || trimmed.endsWith("]}")) {
            // 确保JSON结构完整性
            int openBraces = countChar(trimmed, '{');
            int closeBraces = countChar(trimmed, '}');
            int openBrackets = countChar(trimmed, '[');
            int closeBrackets = countChar(trimmed, ']');
            
            if (closeBraces > openBraces || closeBrackets > openBrackets) {
                // 找到最后一个有效的JSON结构结束
                int lastValidPos = findLastValidJsonEnd(trimmed);
                if (lastValidPos > 0 && lastValidPos < trimmed.length()) {
                    trimmed = trimmed.substring(0, lastValidPos + 1);
                } else {
                    break;  // 无法安全删除，停止处理
                }
            } else {
                break;  // 括号匹配，不需要移除
            }
        }
        
        return trimmed;
    }
    
    /**
     * 查找JSON中最后一个有效的结束位置
     */
    private int findLastValidJsonEnd(String json) {
        try {
            // 尝试解析JSON以找到有效结束
            JSONUtil.parse(json);
            return json.length() - 1;  // 如果可以解析，整个字符串是有效的
        } catch (Exception e) {
            // 从后向前逐步截断，找到可以解析的部分
            for (int i = json.length() - 1; i > 0; i--) {
                if (json.charAt(i) == '}' || json.charAt(i) == ']') {
                    String subJson = json.substring(0, i + 1);
                    try {
                        JSONUtil.parse(subJson);
                        return i;  // 找到有效结束位置
                    } catch (Exception ignored) {
                        // 继续尝试
                    }
                }
            }
        }
        return -1;  // 无法找到有效结束
    }
    
    /**
     * 计算字符串中特定字符的出现次数
     */
    private int countChar(String str, char target) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    /**
     * 修复严重畸形的知识分析JSON
     * @param jsonStr 原始JSON字符串
     * @return 修复后的JSONObject
     */
    private JSONObject fixMalformedKnowledgeAnalysis(String jsonStr) {
        JSONObject result = new JSONObject();
        
        try {
            // 如果是完整的JSON对象，尝试直接解析
            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                try {
                    // 尝试解析整个JSON
                    JSONObject parsed = JSONUtil.parseObj(jsonStr);
                    return parsed;
                } catch (Exception e) {
                    log.debug("JSON解析失败，尝试手动修复: {}", e.getMessage());
                    // 继续使用下面的手动修复方法
                }
            }
            
            // 处理包含重复键的情况
            if (jsonStr.contains("\"errorAnalysis\"") && 
                jsonStr.indexOf("\"errorAnalysis\"") != jsonStr.lastIndexOf("\"errorAnalysis\"")) {
                
                // 只保留第一个errorAnalysis键及其值
                int firstErrorAnalysis = jsonStr.indexOf("\"errorAnalysis\"");
                int secondErrorAnalysis = jsonStr.indexOf("\"errorAnalysis\"", firstErrorAnalysis + 1);
                
                if (firstErrorAnalysis > 0 && secondErrorAnalysis > 0) {
                    // 截取第一个errorAnalysis直到第二个errorAnalysis之前的部分
                    String fixedJson = jsonStr.substring(0, secondErrorAnalysis);
                    // 查找前一个逗号位置
                    int lastComma = fixedJson.lastIndexOf(",");
                    if (lastComma > 0) {
                        fixedJson = fixedJson.substring(0, lastComma);
                    }
                    // 添加结束的大括号
                    fixedJson += "}";
                    
                    try {
                        return JSONUtil.parseObj(fixedJson);
                    } catch (Exception e) {
                        log.debug("修复后的JSON仍然无法解析: {}", e.getMessage());
                    }
                }
            }
            
            // 手动提取各个字段
            // 提取masteryLevel
            String masteryLevel = extractJsonField(jsonStr, "masteryLevel");
            if (StrUtil.isNotBlank(masteryLevel)) {
                result.set("masteryLevel", masteryLevel);
            } else {
                result.set("masteryLevel", "知识掌握情况未能正确解析");
            }
            
            // 提取commonProblems
            String commonProblems = extractJsonField(jsonStr, "commonProblems");
            if (StrUtil.isNotBlank(commonProblems)) {
                result.set("commonProblems", commonProblems);
            } else {
                result.set("commonProblems", "普遍问题未能正确解析");
            }
            
            // 提取errorAnalysis，只提取第一次出现的部分
            String errorAnalysis = extractJsonField(jsonStr, "errorAnalysis");
            if (StrUtil.isNotBlank(errorAnalysis)) {
                // 限制长度，防止数据库字段溢出
                if (errorAnalysis.length() > 1000) {
                    errorAnalysis = errorAnalysis.substring(0, 1000);
                }
                result.set("errorAnalysis", errorAnalysis);
            } else {
                result.set("errorAnalysis", "错误分析未能正确解析");
            }
            
        } catch (Exception e) {
            log.error("修复知识分析JSON失败: {}", e.getMessage());
            // 创建一个默认对象
            result.set("masteryLevel", "知识掌握情况未能正确解析");
            result.set("commonProblems", "普遍问题未能正确解析");
            result.set("errorAnalysis", "错误分析未能正确解析");
        }
        
        return result;
    }

    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractJsonField(String jsonStr, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(jsonStr);
        
        if (m.find()) {
            String value = m.group(1);
            // 检测并修复UTF-8编码问题
            return sanitizeString(value);
        }
        return "";
    }

    /**
     * 清理字符串中的无效字符，确保UTF-8编码有效
     */
    private String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        
        // 替换非法UTF-8字符
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 只保留可打印ASCII字符和常见中文字符
            if ((c >= 32 && c <= 126) || (c >= 0x4E00 && c <= 0x9FFF) || c == '\n' || c == '\r' || c == '\t') {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }

    /**
     * 从知识分析中提取知识点
     * @param analysis 知识分析JSON对象
     * @return 提取的知识点列表
     */
    private List<String> extractKnowledgePointsFromAnalysis(JSONObject analysis) {
        List<String> points = new ArrayList<>();
        
        // 检查raw_knowledge_points字段（可能AI直接返回了知识点）
        if (analysis.containsKey("raw_knowledge_points")) {
            Object rawPoints = analysis.get("raw_knowledge_points");
            if (rawPoints instanceof JSONArray) {
                JSONArray pointsArray = (JSONArray) rawPoints;
                for (int i = 0; i < pointsArray.size() && points.size() < 5; i++) {
                    String point = pointsArray.getStr(i);
                    if (point != null && !point.isEmpty() && point.length() < 10) {
                        points.add(point);
                    }
                }
                return points; // 如果找到了，直接返回
            }
        }
        
        // 根据学科提取相关概念
        String subjectField = "";
        if (analysis.containsKey("subject")) {
            subjectField = analysis.getStr("subject", "");
        }
        
        // 从错误分析和常见问题中查找关键概念
        String combinedText = "";
        if (analysis.containsKey("errorAnalysis")) {
            combinedText += analysis.getStr("errorAnalysis") + " ";
        }
        if (analysis.containsKey("commonProblems")) {
            combinedText += analysis.getStr("commonProblems") + " ";
        }
        if (analysis.containsKey("masteryLevel")) {
            combinedText += analysis.getStr("masteryLevel") + " ";
        }
        
        // 根据学科类型提取相关概念
        boolean isMath = subjectField.contains("数学") || 
                         combinedText.contains("函数") || 
                         combinedText.contains("方程") || 
                         combinedText.contains("几何");
        
        boolean isPhysics = subjectField.contains("物理") || 
                             combinedText.contains("力学") || 
                             combinedText.contains("电磁") || 
                             combinedText.contains("热学");
        
        boolean isChemistry = subjectField.contains("化学") || 
                               combinedText.contains("元素") || 
                               combinedText.contains("反应") || 
                               combinedText.contains("分子");
        
        boolean isEnglish = subjectField.contains("英语") || 
                             combinedText.contains("词汇") || 
                             combinedText.contains("语法") || 
                             combinedText.contains("阅读");
        
        boolean isChinese = subjectField.contains("语文") || 
                             combinedText.contains("文学") || 
                             combinedText.contains("写作") || 
                             combinedText.contains("阅读");
        
        // 根据识别的学科类型选择合适的概念列表
        String[] concepts;
        if (isMath) {
            // 定义精确的数学概念列表 - 按专业术语长度排序（短的先匹配）
            concepts = new String[] {
                // 基础数学概念
                "极限", "函数", "导数", "积分", "方程", "不等式", "矩阵", "向量", "数列", 
                // 几何相关
                "几何", "立体几何", "解析几何", "平面几何", "空间向量", 
                // 三角函数相关
                "三角函数", "正弦函数", "余弦函数", "正切函数", 
                // 概率统计
                "概率", "统计", "随机变量", "分布", 
                // 代数
                "多项式", "因式分解", "二项式", "对数", "对数函数", "指数函数",
                // 具体技能点
                "函数单调性", "函数周期性", "函数奇偶性", "函数图像", "求导", "微分", "积分计算",
                "三角恒等式", "数列求和", "数学归纳法", "方程求解", "不等式证明"
            };
        } else if (isPhysics) {
            concepts = new String[] {
                "力学", "运动学", "动力学", "牛顿定律", "功能", "能量守恒", 
                "电场", "磁场", "电磁感应", "电路", "光学", "波动", "热学",
                "气体定律", "热力学", "相对论", "量子力学", "原子物理"
            };
        } else if (isChemistry) {
            concepts = new String[] {
                "元素", "周期表", "化学键", "分子结构", "化学反应", "氧化还原",
                "酸碱中和", "电解质", "有机化学", "无机化学", "化学平衡", 
                "反应速率", "催化剂", "热化学", "电化学", "溶液"
            };
        } else if (isEnglish) {
            concepts = new String[] {
                "词汇", "语法", "时态", "语态", "情态动词", "条件句", 
                "阅读理解", "写作", "听力", "口语", "翻译", "完形填空"
            };
        } else if (isChinese) {
            concepts = new String[] {
                "文学常识", "古代文学", "现代文学", "文言文", "诗词鉴赏", 
                "散文", "小说", "议论文", "记叙文", "说明文", "写作技巧", 
                "修辞手法", "语法", "文言实词", "文言虚词"
            };
        } else {
            // 默认使用基础概念列表
            concepts = new String[] {
                "基础概念", "核心原理", "关键技能", "应用方法", "解题思路", 
                "常见错误", "学习策略", "知识体系", "学科素养"
            };
        }
        
        // 提取概念
        for (String concept : concepts) {
            if (combinedText.contains(concept) && !points.contains(concept)) {
                points.add(concept);
                if (points.size() >= 5) break;
            }
        }
        
        // 如果没有匹配到足够的概念，尝试更通用的模式匹配
        if (points.isEmpty()) {
            // 匹配形如"xxx的xxx"或"xxx性质"的模式
            Pattern conceptPattern = Pattern.compile("(([\\u4e00-\\u9fa5]{1,5}的[\\u4e00-\\u9fa5]{1,5}性)|([\\u4e00-\\u9fa5]{1,5}函数)|([\\u4e00-\\u9fa5]{1,3}几何)|([\\u4e00-\\u9fa5]{1,4}数列)|([\\u4e00-\\u9fa5]{1,4}方程)|([\\u4e00-\\u9fa5]{1,4}不等式)|([\\u4e00-\\u9fa5]{1,5}性质))");
            Matcher matcher = conceptPattern.matcher(combinedText);
            while (matcher.find() && points.size() < 5) {
                String concept = matcher.group().trim();
                if (!points.contains(concept) && concept.length() < 8) {
                    points.add(concept);
                }
            }
        }
        
        // 去除点、数字和空格
        for (int i = 0; i < points.size(); i++) {
            String point = points.get(i);
            // 去除数字、点、空格等干扰字符
            point = point.replaceAll("[0-9.．。、,:：；;\\s]+", "");
            // 去除常见的非核心前缀
            point = point.replaceAll("^(关于|在|对于|学习|理解|掌握|应用)", "");
            // 如果处理后太短，则移除
            if (point.length() < 2) {
                points.remove(i);
                i--;
                continue;
            }
            points.set(i, point);
        }
        
        return points;
    }
} 