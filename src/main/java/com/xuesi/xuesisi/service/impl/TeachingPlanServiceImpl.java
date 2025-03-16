package com.xuesi.xuesisi.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.TeachingPlanMapper;
import com.xuesi.xuesisi.model.entity.*;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/15
 **/
@Service
@Slf4j
public class TeachingPlanServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan>
        implements TeachingPlanService {

    @Resource
    private UserAnswerService userAnswerService;
    
    @Resource
    private QuestionService questionService;
    
    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private QuestionBankService questionBankService;
    
    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Override
    public Long generateTeachingPlan(Long userAnswerId) {
        log.info("开始生成教案，答题记录ID：{}", userAnswerId);
        
        // 1. 获取答题记录
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null) {
            log.error("答题记录不存在，ID：{}", userAnswerId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答题记录不存在");
        }
        
        try {
            // 2. 获取题目列表
            List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.lambdaQuery()
                .eq(QuestionBankQuestion::getQuestionBankId, userAnswer.getQuestionBankId())
                .orderByAsc(QuestionBankQuestion::getQuestionOrder)
                .list();
            log.info("找到题目关联记录数量：{}", questionBankQuestions.size());
                
            List<Long> questionIds = questionBankQuestions.stream()
                .map(QuestionBankQuestion::getQuestionId)
                .collect(java.util.stream.Collectors.toList());
            log.info("题目ID列表：{}", questionIds);
                
            List<Question> questions = questionService.listByIds(questionIds);
            log.info("获取到题目数量：{}", questions.size());
            
            // 3. 获取错题的知识点
            List<String> choices = JSONUtil.parseArray(userAnswer.getChoices()).toList(String.class);
            log.info("用户答案列表：{}", choices);
            List<String> wrongPoints = new ArrayList<>();
            
            for (int i = 0; i < questions.size() && i < choices.size(); i++) {
                Question question = questions.get(i);
                String userChoice = choices.get(i);
                List<String> correctAnswer = JSONUtil.parseArray(question.getAnswerStr()).toList(String.class);
                log.info("题目{}：用户答案={}，正确答案={}", i + 1, userChoice, correctAnswer);
                
                // 判断答案是否正确
                boolean isCorrect = false;
                if (question.getQuestionType() == 0) { // 单选题
                    isCorrect = correctAnswer != null && !correctAnswer.isEmpty() && 
                              userChoice != null && correctAnswer.get(0).equals(userChoice);
                } else if (question.getQuestionType() == 1) { // 多选题
                    List<String> userChoices = userChoice != null ? 
                        userChoice.chars()
                            .mapToObj(ch -> String.valueOf((char) ch))
                            .collect(java.util.stream.Collectors.toList()) : 
                        new ArrayList<>();
                    isCorrect = correctAnswer != null && 
                        new java.util.HashSet<>(correctAnswer).equals(new java.util.HashSet<>(userChoices));
                }
                log.info("题目{}：答案{}正确", i + 1, isCorrect ? "是" : "不");
                
                // 如果答错了，添加该题目的知识点
                if (!isCorrect && question.getTagsStr() != null) {
                    List<String> tags = JSONUtil.parseArray(question.getTagsStr()).toList(String.class);
                    wrongPoints.addAll(tags);
                    log.info("题目{}：添加知识点 {}", i + 1, tags);
                }
            }
            
            log.info("收集到的错题知识点：{}", wrongPoints);
            
            // 4. 构造AI提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下信息生成一份详细的教学教案：\n");
            prompt.append("1. 学生在以下知识点表现欠佳：").append(String.join("、", wrongPoints)).append("\n");
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
            
            // 5. 调用AI生成教案
            log.info("开始调用AI生成教案");
            String aiTeachingPlan = deepSeekService.chat(prompt.toString());
            log.info("AI返回的教案：{}", aiTeachingPlan);
            
            // 6. 解析AI返回的教案
            TeachingPlan plan = new TeachingPlan();
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
            
            // 7. 保存教案
            save(plan);
            log.info("教案生成并保存成功，ID：{}", plan.getId());
            
            return plan.getId();
        } catch (Exception e) {
            log.error("生成教案失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成教案失败：" + e.getMessage());
        }
    }
}
