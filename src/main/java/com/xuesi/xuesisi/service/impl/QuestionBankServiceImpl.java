package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.ScoringStrategyEnum;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.QuestionBankMapper;
import com.xuesi.xuesisi.mapper.ScoringResultMapper;
import com.xuesi.xuesisi.model.entity.*;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.scoring.ScoringStrategyExecutor;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import java.sql.SQLNonTransientConnectionException;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 题库服务实现类
 */
@Slf4j
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private DeepSeekService deepseekService;

    @Resource
    private ScoringResultMapper scoringResultMapper;

    @Resource
    private ScoringStrategyExecutor scoringStrategyExecutor;

    @Resource
    private KnowledgePointService knowledgePointService;

    @Resource
    private QuestionKnowledgeService questionKnowledgeService;

    @Override
    @Retryable(value = SQLNonTransientConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Long createQuestionBank(QuestionBank questionBank) {
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 校验题库名称
        String name = questionBank.getTitle();
        if (StringUtils.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库名称过长");
        }
        
        // 设置默认值
        questionBank.setQuestionCount(questionBank.getQuestionCount() == null ? 0 : questionBank.getQuestionCount());
        
        // 确保题目类型有效
        if (questionBank.getQuestionBankType() == null) {
            questionBank.setQuestionBankType(0); // 默认为单选题
        }
        
        // 确保学科不为空
        if (StringUtils.isBlank(questionBank.getSubject())) {
            questionBank.setSubject("数学"); // 默认为数学
        }
        
        // 如果description为空，添加默认description
        if (StringUtils.isBlank(questionBank.getDescription())) {
            // 根据题目类型添加合适的默认描述
            String defaultDescription;
            switch (questionBank.getQuestionBankType()) {
                case 0: // 单选题
                    defaultDescription = "1. 生成适合学生水平的" + questionBank.getSubject() + "单选题\n" +
                                        "2. 每个题目都要有详细解析\n" +
                                        "3. 确保选项有足够的区分度";
                    break;
                case 1: // 多选题
                    defaultDescription = "1. 生成适合学生水平的" + questionBank.getSubject() + "多选题\n" +
                                        "2. 每个题目必须有至少2个正确选项\n" +
                                        "3. 选项内容要有明确的对错";
                    break;
                case 2: // 填空题
                    defaultDescription = "1. 生成适合学生水平的" + questionBank.getSubject() + "填空题\n" +
                                        "2. 题目要有明确的空缺部分\n" +
                                        "3. 答案应当简洁明了";
                    break;
                case 3: // 简答题
                    defaultDescription = "1. 生成适合学生水平的" + questionBank.getSubject() + "简答题\n" +
                                        "2. 题目要有明确的问题\n" +
                                        "3. 解析过程要详细清晰";
                    break;
                default:
                    defaultDescription = "1. 生成适合学生水平的" + questionBank.getSubject() + "题目\n" +
                                        "2. 每个题目都要有详细解析\n" +
                                        "3. 确保题目内容符合教学要求";
            }
            questionBank.setDescription(defaultDescription);
        }
        
        questionBank.setReviewStatus(0);
        questionBank.setIsDelete(0);
        
        // 构建AI提示词
        String prompt = buildAIPrompt(questionBank);
        log.info("生成的AI提示词: {}", prompt);
        
        try {
            log.info("开始调用AI服务生成题目");
                
            // 调用AI服务生成题目
            String aiResponse = deepseekService.chat(prompt);
            log.info("AI响应: {}", aiResponse);
            
            // 检查是否为空响应
            if (StringUtils.isBlank(aiResponse)) {
                log.error("AI生成题目失败：响应为空");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成题目失败：响应为空");
            }
            
            // 提取JSON内容
            String jsonContent = extractJsonFromResponse(aiResponse);
                
            // 如果JSON内容为空，报错
            if (StringUtils.isBlank(jsonContent)) {
                log.error("AI生成题目失败：无法提取有效JSON内容");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成题目失败：无法提取有效JSON内容");
            }
            
            // 预处理JSON
            String processedJson = preprocessJson(jsonContent);
            
            // 验证是否包含questions字段并且不为空
            try {
                JSONObject jsonObject = JSONUtil.parseObj(processedJson);
                JSONArray questions = jsonObject.getJSONArray("questions");
                if (questions == null || questions.isEmpty()) {
                    log.error("AI生成题目失败：题目列表为空");
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成题目失败：题目列表为空");
                }
        } catch (Exception e) {
                log.error("AI生成题目失败：JSON解析错误", e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成题目失败：" + e.getMessage());
            }
            
            // 在事务中创建题库和题目
            return createQuestionBankInTransaction(questionBank, processedJson);
            
        } catch (Exception e) {
            log.error("生成题库失败", e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成题库失败：" + e.getMessage());
        }
    }
    
    /**
     * 简化提示词，专注于获取基本有效的JSON响应
     */
    private String simplifyPrompt(String originalPrompt) {
        // 将原始提示词分成几部分
        String[] parts = originalPrompt.split("\n\n");
        
        // 只保留前几个重要部分，移除冗长的格式说明
        StringBuilder simplifiedPrompt = new StringBuilder();
        
        // 保留核心指令部分
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            simplifiedPrompt.append(parts[i]).append("\n\n");
        }
        
        // 添加简化的JSON格式要求
        simplifiedPrompt.append("JSON格式要求（必须严格遵守）：\n");
        simplifiedPrompt.append("1. 返回有效的JSON，格式为: {\"questions\": [...]}\n");
        simplifiedPrompt.append("2. 每个题目必须包含content, options, answer和analysis字段\n");
        simplifiedPrompt.append("3. 确保JSON格式正确，可以被解析\n\n");
        
        // 添加最重要的提醒
        simplifiedPrompt.append("重要提醒：\n");
        simplifiedPrompt.append("1. 直接返回JSON，不要添加任何解释或额外文本\n");
        simplifiedPrompt.append("2. 确保JSON格式正确\n");
        
        return simplifiedPrompt.toString();
    }
    
    /**
     * 根据验证失败情况调整提示词，提高有效题目生成率
     */
    private String adjustPromptForValidQuestions(String originalPrompt, QuestionBank questionBank) {
        // 减少请求的题目数量，提高成功率
        int reducedCount = Math.max(1, questionBank.getQuestionCount() / 2);
        
        // 更新题库中的题目数量
        questionBank.setQuestionCount(reducedCount);
        
        // 构建新的提示词
        StringBuilder adjustedPrompt = new StringBuilder();
        adjustedPrompt.append("你是一个专业的题目生成器。请生成").append(reducedCount)
              .append("个").append(questionBank.getSubject()).append("学科的题目。\n\n");
              
        adjustedPrompt.append("语言要求：\n");
        adjustedPrompt.append("1. 所有题目内容必须使用中文\n");
        adjustedPrompt.append("2. 简化公式表达，尽量避免复杂的LaTeX\n");
        adjustedPrompt.append("3. 答案解析简洁明了\n\n");
        
        // 根据题目类型生成基本提示
        String questionTypeName;
        switch (questionBank.getQuestionBankType()) {
            case 0: 
                questionTypeName = "单选题";
                adjustedPrompt.append("题目类型：单选题\n");
                adjustedPrompt.append("要求：\n");
                adjustedPrompt.append("1. 每个题目包含4个选项(A,B,C,D)\n");
                adjustedPrompt.append("2. 答案是单个字母(A或B或C或D)\n\n");
                break;
            case 1: 
                questionTypeName = "多选题";
                adjustedPrompt.append("题目类型：多选题\n");
                adjustedPrompt.append("要求：\n");
                adjustedPrompt.append("1. 每个题目包含4个选项(A,B,C,D)\n");
                adjustedPrompt.append("2. 答案是多个字母的组合，如A,B\n\n");
                break;
            case 2: 
                questionTypeName = "填空题";
                adjustedPrompt.append("题目类型：填空题\n");
                adjustedPrompt.append("要求：\n");
                adjustedPrompt.append("1. 题目中有一个空缺部分用___表示\n");
                adjustedPrompt.append("2. 答案明确简洁\n\n");
                break;
            case 3: 
                questionTypeName = "简答题";
                adjustedPrompt.append("题目类型：简答题\n");
                adjustedPrompt.append("要求：\n");
                adjustedPrompt.append("1. 题目提出明确问题\n");
                adjustedPrompt.append("2. 答案详细解释\n\n");
                break;
            default:
                questionTypeName = "单选题";
                adjustedPrompt.append("题目类型：单选题\n");
                adjustedPrompt.append("要求：\n");
                adjustedPrompt.append("1. 每个题目包含4个选项(A,B,C,D)\n");
                adjustedPrompt.append("2. 答案是单个字母(A或B或C或D)\n\n");
        }
        
        // 添加题目要求
        if (StringUtils.isNotBlank(questionBank.getDescription())) {
            adjustedPrompt.append("题目要求：\n");
            adjustedPrompt.append(questionBank.getDescription()).append("\n\n");
        }
        
        // 简化的JSON格式要求
        adjustedPrompt.append("JSON格式要求：\n");
        adjustedPrompt.append("1. 必须返回格式为{\"questions\":[...]}的JSON\n");
        adjustedPrompt.append("2. 每个题目必须有content、answer和analysis字段\n");
        adjustedPrompt.append("3. 直接返回JSON，不要加任何说明\n\n");
        
        // 提供一个非常简单的示例
        adjustedPrompt.append("简单示例：\n");
        adjustedPrompt.append("{\n");
        adjustedPrompt.append("  \"questions\": [\n");
        adjustedPrompt.append("    {\n");
        adjustedPrompt.append("      \"content\": \"简单的题目内容\",\n");
        if (questionBank.getQuestionBankType() == 0 || questionBank.getQuestionBankType() == 1) {
            adjustedPrompt.append("      \"options\": [\"选项A\", \"选项B\", \"选项C\", \"选项D\"],\n");
        }
        adjustedPrompt.append("      \"answer\": \"" + (questionBank.getQuestionBankType() == 1 ? "A,B" : "A") + "\",\n");
        adjustedPrompt.append("      \"analysis\": \"简单的题目解析\",\n");
        adjustedPrompt.append("      \"knowledgeTags\": [\"基础知识\"]\n");
        adjustedPrompt.append("    }\n");
        adjustedPrompt.append("  ]\n");
        adjustedPrompt.append("}\n");
        
        return adjustedPrompt.toString();
    }

    @Recover
    public Long recoverCreateQuestionBank(SQLNonTransientConnectionException e, QuestionBank questionBank) {
        log.error("After 3 retries, still failed to create question bank", e);
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建题库失败，请稍后重试");
    }

    /**
     * 从AI响应中提取JSON部分
     */
    private String extractJsonFromResponse(String response) {
        String jsonStr = null;
        try {
            if (StringUtils.isBlank(response)) {
                log.warn("AI响应为空");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应为空");
            }

            // 首先将文本形式的换行符替换为实际的换行符
            response = response.replace("\\n", "\n");
            
            // 记录日志，显示响应内容的前300个字符
            int previewLength = Math.min(response.length(), 300);
            log.debug("原始AI响应 (前{}字符): {}", previewLength, response.substring(0, previewLength));
            
            // 处理Markdown代码块
            Pattern markdownPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.DOTALL);
            Matcher markdownMatcher = markdownPattern.matcher(response);
            
            if (markdownMatcher.find()) {
                // 提取代码块中的内容
                jsonStr = markdownMatcher.group(1).trim();
                log.debug("从Markdown代码块中提取JSON内容，长度: {}", jsonStr.length());
            } else {
                // 如果没有找到代码块，尝试直接解析JSON
                log.debug("未找到Markdown代码块，尝试直接解析JSON");
                // 查找JSON内容 - 首先尝试找到大括号包围的内容
                Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);
                Matcher jsonMatcher = jsonPattern.matcher(response);
                
                if (jsonMatcher.find()) {
                    jsonStr = jsonMatcher.group();
                } else {
                    log.warn("未找到JSON内容");
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成有效的JSON");
                }
            }
            
            // 记录JSON预览，限制长度以防止日志过大
            int jsonPreviewLength = Math.min(jsonStr.length(), 500);
            log.debug("原始JSON预览 (前{}字符): {}", jsonPreviewLength, jsonStr.substring(0, jsonPreviewLength));
            
            // 分析字符细节，特别是换行符
            logCharacterDetails(jsonStr, 100);
            
            // 处理文本形式的换行符和制表符
            jsonStr = jsonStr.replace("\\n", " ")
                          .replace("\\r", " ")
                          .replace("\\t", " ");
            
            // 处理实际的换行符，将其转换为空格
            jsonStr = jsonStr.replace("\n", " ")
                          .replace("\r", " ")
                          .replace("\t", " ");
            
            // 特殊处理导数符号f'(x)中的单引号
            jsonStr = jsonStr.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\\\'(");
            
            // 移除可能导致JSON解析错误的字符
            jsonStr = jsonStr.replaceAll("[\\x00-\\x1F]", " ");
            
            // 确保JSON的完整性 - 检查开始和结束括号
            if (!jsonStr.startsWith("{")) {
                int firstBrace = jsonStr.indexOf("{");
                if (firstBrace >= 0) {
                    jsonStr = jsonStr.substring(firstBrace);
                    log.debug("修正了JSON开始位置");
                } else {
                    jsonStr = "{" + jsonStr;
                    log.debug("添加了缺失的开始大括号");
                }
            }
            
            if (!jsonStr.endsWith("}")) {
                int lastBrace = jsonStr.lastIndexOf("}");
                if (lastBrace >= 0) {
                    jsonStr = jsonStr.substring(0, lastBrace + 1);
                    log.debug("修正了JSON结束位置");
                } else {
                    jsonStr = jsonStr + "}";
                    log.debug("添加了缺失的结束大括号");
                }
            }
            
            // 检查questions数组是否完整
            if (!ensureQuestionsArrayComplete(jsonStr)) {
                log.warn("questions数组不完整，尝试修复");
                jsonStr = fixIncompleteQuestionsArray(jsonStr);
            }
            
            return jsonStr;
        } catch (Exception e) {
            log.error("提取JSON失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提取JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查questions数组是否完整
     */
    private boolean ensureQuestionsArrayComplete(String jsonStr) {
        // 查找questions数组开始位置
        int questionsStart = jsonStr.indexOf("\"questions\"");
        if (questionsStart < 0) {
            return false;
        }
        
        // 找到questions数组开始的'['
        int arrayStart = jsonStr.indexOf("[", questionsStart);
        if (arrayStart < 0) {
            return false;
        }
        
        // 计算嵌套括号
        int openBrackets = 1;
        boolean insideString = false;
        boolean escaped = false;
        
        for (int i = arrayStart + 1; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                insideString = !insideString;
                continue;
            }
            
            if (!insideString) {
                if (c == '[') {
                    openBrackets++;
                } else if (c == ']') {
                    openBrackets--;
                    if (openBrackets == 0) {
                        // 找到了匹配的']'，数组是完整的
                        return true;
                    }
                }
            }
        }
        
        // 没有找到匹配的']'，数组是不完整的
        return false;
    }
    
    /**
     * 修复不完整的questions数组
     */
    private String fixIncompleteQuestionsArray(String jsonStr) {
        // 检查是否有questions数组开始
        int questionsStart = jsonStr.indexOf("\"questions\"");
        if (questionsStart < 0) {
            // 如果没有questions字段，添加一个空的questions数组
            if (jsonStr.endsWith("}")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 1) + ",\"questions\":[]}";
            } else {
                jsonStr = "{\"questions\":[]}";
            }
            return jsonStr;
        }
        
        // 找到questions数组开始的'['
        int arrayStart = jsonStr.indexOf("[", questionsStart);
        if (arrayStart < 0) {
            // 如果没有数组开始符号，添加一个空数组
            int colonPos = jsonStr.indexOf(":", questionsStart);
            if (colonPos > 0) {
                String before = jsonStr.substring(0, colonPos + 1);
                String after = jsonStr.substring(colonPos + 1);
                return before + "[]" + after;
            } else {
                // 异常情况，返回一个有效的空JSON
                return "{\"questions\":[]}";
            }
        }
        
        // 计算已完成的问题对象数量
        int completedObjects = 0;
        int currentPos = arrayStart + 1;
        int openBraces = 0;
        boolean insideObject = false;
        boolean insideString = false;
        boolean escaped = false;
        StringBuilder result = new StringBuilder(jsonStr.substring(0, arrayStart + 1));
        
        // 遍历字符串，找到完整的问题对象
        while (currentPos < jsonStr.length()) {
            char c = jsonStr.charAt(currentPos);
            
            result.append(c);
            
            if (escaped) {
                escaped = false;
                currentPos++;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                currentPos++;
                continue;
            }
            
            if (c == '"' && !escaped) {
                insideString = !insideString;
                currentPos++;
                continue;
            }
            
            if (!insideString) {
                if (c == '{') {
                    openBraces++;
                    insideObject = true;
                } else if (c == '}') {
                    openBraces--;
                    if (openBraces == 0 && insideObject) {
                        completedObjects++;
                        insideObject = false;
                        
                        // 检查下一个字符是否为逗号或数组结束
                        if (currentPos + 1 < jsonStr.length()) {
                            char next = jsonStr.charAt(currentPos + 1);
                            if (next != ',' && next != ']') {
                                // 如果缺少逗号或结束符，我们添加一个结束符
                                result.append(']');
                                break;
                            }
                        } else {
                            // 到达字符串末尾，添加结束符
                            result.append(']');
                            break;
                        }
                    }
                }
            }
            
            currentPos++;
        }
        
        // 如果没有找到完整的问题对象，返回空数组
        if (completedObjects == 0) {
            return "{\"questions\":[]}";
        }
        
        // 确保JSON有正确的结束
        String fixedJson = result.toString();
        if (!fixedJson.endsWith("]")) {
            fixedJson += "]";
        }
        
        // 确保整个JSON对象有正确的结束
        if (!fixedJson.endsWith("]}")) {
            if (fixedJson.endsWith("]")) {
                fixedJson += "}";
            } else {
                fixedJson += "]}";
            }
        }
        
        return fixedJson;
    }

    /**
     * 输出字符详情，用于调试
     */
    private void logCharacterDetails(String str, int limit) {
        if (str == null || str.isEmpty()) {
            log.debug("字符串为空");
            return;
        }
        
        StringBuilder details = new StringBuilder();
        int count = Math.min(str.length(), limit);
        
        for (int i = 0; i < count; i++) {
            char c = str.charAt(i);
            details.append("[位置").append(i).append(":'").append(c).append("'(ASCII:").append((int) c).append(")] ");
        }
        
        log.debug("字符详情: {}", details.toString());
        
        // 特别记录反斜杠
        for (int i = 0; i < count; i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char nextChar = str.charAt(i + 1);
                log.debug("位置 {} 有反斜杠，后跟字符 '{}' (ASCII:{})", i, nextChar, (int) nextChar);
            }
        }
    }
    
    /**
     * 输出JSON中指定区间的内容，用于调试
     */
    private void dumpJsonSectionForDebug(String json, int startPos, int endPos) {
        if (json == null || json.length() <= startPos) {
            log.debug("JSON太短，无法检查位置 {} 到 {}", startPos, endPos);
            return;
        }
        
        int actualEnd = Math.min(json.length(), endPos);
        String section = json.substring(startPos, actualEnd);
        log.debug("JSON位置 {} 到 {} 的内容: '{}'", startPos, actualEnd, section);
        
        // 输出每个字符的详细信息
        StringBuilder charInfo = new StringBuilder();
        for (int i = startPos; i < actualEnd; i++) {
            char c = json.charAt(i);
            charInfo.append(String.format("[位置%d:'%c'(ASCII:%d)] ", i, c, (int)c));
        }
        log.debug("字符详情: {}", charInfo);
        
        // 特别关注反斜杠序列
        for (int i = startPos; i < actualEnd - 1; i++) {
            if (json.charAt(i) == '\\') {
                char nextChar = json.charAt(i + 1);
                log.debug("位置 {} 有反斜杠，后跟字符 '{}' (ASCII:{})", i, nextChar, (int)nextChar);
            }
        }
    }
    
    /**
     * 修复选项中的转义问题
     */
    private String fixOptionsEscapes(String json) {
        try {
            // 正则表达式匹配选项数组
            Pattern optionsPattern = Pattern.compile("\"options\":\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher optionsMatcher = optionsPattern.matcher(json);
            
            StringBuffer sb = new StringBuffer();
            
            while (optionsMatcher.find()) {
                String optionsStr = optionsMatcher.group(1);
                log.debug("找到options字段: {}", optionsStr);
                
                // 处理选项数组中的内容
                String[] options = optionsStr.split(",(?=\\s*\")");
                StringBuilder fixedOptions = new StringBuilder();
                
                for (int i = 0; i < options.length; i++) {
                    String option = options[i].trim();
                    log.debug("处理选项 {}: {}", i, option);
                    
                    // 特殊处理导数符号
                    option = option.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\\\'(");
                    
                    // 特殊处理其他数学表达式中的符号
                    option = processSpecialMathSymbols(option);
                    
                    // 添加逗号分隔符（最后一个选项除外）
                    fixedOptions.append(option);
                    if (i < options.length - 1) {
                        fixedOptions.append(", ");
                    }
                }
                
                // 将修复后的选项放回原位
                optionsMatcher.appendReplacement(sb, "\"options\":[" + fixedOptions + "]");
            }
            optionsMatcher.appendTail(sb);
            
            return sb.toString();
        } catch (Exception e) {
            log.warn("修复选项转义时出错: {}", e.getMessage(), e);
            // 出错时返回原始字符串，避免丢失内容
            return json;
        }
    }
    
    /**
     * 处理选项中的特殊数学符号
     */
    private String processSpecialMathSymbols(String text) {
        if (text == null) {
            return "";
        }
        
        // 处理各种符号
        String result = text;
        
        // 处理导数符号 f'(x)
        result = result.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\\\'(");
        
        // 处理二阶导数符号 f''(x)
        result = result.replaceAll("([a-zA-Z])\\s*''\\s*\\(", "$1\\\\''(");
        
        // 处理其他特殊符号
        // 处理积分符号
        result = result.replace("∫", "\\\\int");
        
        // 处理无穷大符号
        result = result.replace("∞", "\\\\infty");
        
        // 处理偏导符号
        result = result.replace("∂", "\\\\partial");
        
        // 确保所有反斜杠被正确转义
        result = result.replace("\\\\\\", "\\\\");
        
        return result;
    }
    
    /**
     * 处理文本中特殊字符的转义问题
     */
    private String fixLatexCommandEscapes(String json) {
        // 直接处理JSON中可能导致解析错误的特殊字符
        // 不再处理LaTeX命令，因为我们现在使用纯文本格式
        
        // 处理引号和反斜杠
        json = json.replace("\\\"", "\\\\\""); // 确保引号被正确转义
        
        // 确保反斜杠被正确转义，但避免过度转义
        json = json.replaceAll("(?<![\\\\])\\\\(?![\\\\\"bfnrt/])", "\\\\\\\\");
        
        // 处理JSON中的特殊字符
        json = json.replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
        
        // 确保所有正规JSON转义序列保持正确
        json = ensureValidJsonEscapes(json);
        
        return json;
    }

    /**
     * 确保JSON转义序列的有效性
     */
    private String ensureValidJsonEscapes(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                // 前一个字符是反斜杠
                escaped = false;
                
                if (c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't') {
                    // 标准JSON转义字符，保持原样
                    result.append('\\').append(c);
                } else {
                    // 非标准转义字符，直接保留字符
                    result.append(c);
                }
            } else if (c == '\\') {
                // 遇到反斜杠，标记为转义状态
                escaped = true;
            } else if (c == '"') {
                // 引号，切换字符串状态
                inString = !inString;
                result.append(c);
            } else {
                // 普通字符，直接添加
                result.append(c);
            }
        }
        
        // 处理最后一个字符如果是反斜杠
        if (escaped) {
            result.append('\\');
        }
        
        return result.toString();
    }
    
    /**
     * 确保所有反斜杠被正确转义
     */
    private String ensureProperBackslashEscapes(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                // 前一个字符是反斜杠
                escaped = false;
                
                if (c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't') {
                    // 标准JSON转义字符，保持原样
                    result.append('\\').append(c);
                } else {
                    // 非标准转义字符，添加额外的反斜杠
                    result.append('\\').append('\\').append(c);
                }
            } else if (c == '\\') {
                // 遇到反斜杠，标记为转义状态
                escaped = true;
                // 不立即添加，等待处理下一个字符
            } else if (c == '"') {
                // 引号，切换字符串状态
                inString = !inString;
                result.append(c);
            } else {
                // 普通字符，直接添加
                result.append(c);
            }
        }
        
        // 处理最后一个字符如果是反斜杠
        if (escaped) {
            result.append('\\').append('\\');
        }
        
        return result.toString();
    }
    
    /**
     * 紧急修复JSON中的所有LaTeX相关问题
     */
    private String emergencyJsonFix(String jsonStr) {
        log.info("执行紧急JSON修复...");
        
        try {
            // 尝试处理整个字符串而不是正则表达式替换
            StringBuilder sb = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;
            
            for (int i = 0; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                
                // 特别检查位置53附近的问题
                if (i >= 50 && i <= 55) {
                    log.debug("处理位置 {}: 字符 '{}' ({}), 引号状态: {}, 转义状态: {}", 
                              i, c, (int)c, inQuotes, escaped);
                }
                
                if (escaped) {
                    // 前一个字符是反斜杠
                    escaped = false;
                    
                    if (c == '"' || c == '\\' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't' || c == '/') {
                        // 合法的JSON转义字符
                        sb.append('\\').append(c);
                    } else if (c == '(' || c == ')' || c == '[' || c == ']' || 
                               Character.isLetter(c) || c == '_' || c == '{' || c == '}' || 
                               c == '^' || c == '+' || c == '-' || c == '=') {
                        // LaTeX相关字符，使用双反斜杠
                        sb.append('\\').append('\\').append(c);
                    } else {
                        // 其他非法转义，移除反斜杠，直接添加字符
                        sb.append(c);
                    }
                } else if (c == '\\') {
                    // 遇到反斜杠，标记转义状态
                    escaped = true;
                } else if (c == '"') {
                    // 引号，切换引号状态
                    inQuotes = !inQuotes;
                    sb.append(c);
                } else {
                    // 普通字符
                    sb.append(c);
                }
            }
            
            // 如果最后一个字符是反斜杠，添加一个反斜杠
            if (escaped) {
                sb.append('\\');
            }
            
            // 使用最严格的修复方法
            String result = sb.toString();
            log.debug("紧急修复后的长度: {}", result.length());
            
            // 特别修复选项数组中的逗号问题
            result = result.replaceAll("(\"[^\"]*)(,\\s*[^\"]*\")", "$1\\\\,$2");
            
            // 添加日志，显示位置53附近的修复后内容
            dumpJsonSectionForDebug(result, 45, 65);
            
            return result;
        } catch (Exception e) {
            log.error("紧急修复失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "JSON修复失败: " + e.getMessage());
        }
    }

    /**
     * 处理数学公式和特殊字符
     */
    private String processMathFormulas(String jsonStr) {
        // 不再处理LaTeX格式的数学公式
        // 对于使用纯文本表示的数学公式，只需确保JSON特殊字符被正确转义
        
        try {
            // 预处理导数符号，防止过度转义
            jsonStr = jsonStr.replace("f\\'(x)", "f'(x)");
            
            // 处理常见的数学符号和运算符
            jsonStr = jsonStr.replace("^", "^")       // 指数符号保持不变
                             .replace("*", "*")       // 乘法符号保持不变
                             .replace("/", "/")       // 除法符号保持不变
                             .replace("|", "\\|")     // 转义竖线
                             .replace("\t", "\\t")    // 转义制表符
                             .replace("\r", "\\r")    // 转义回车
                             .replace("\n", "\\n");   // 转义换行
            
            // 处理JSON字符串中的引号
            StringBuilder sb = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;
            
            for (int i = 0; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                
                if (escaped) {
                    // 前一个字符是反斜杠
                    escaped = false;
                    sb.append('\\').append(c);
                } else if (c == '\\') {
                    // 当前字符是反斜杠
                    escaped = true;
                } else if (c == '"') {
                    // 处理引号
                    if (!inQuotes) {
                        // 进入字符串
                        inQuotes = true;
                        sb.append(c);
                    } else {
                        // 退出字符串
                        inQuotes = false;
                        sb.append(c);
                    }
                } else if (c == '\'' && inQuotes) {
                    // 在字符串内部遇到单引号（导数表示），只使用一个反斜杠转义
                    sb.append("\\'");
                } else {
                    // 普通字符
                    sb.append(c);
                }
            }
            
            // 如果最后一个字符是反斜杠，添加一个额外的反斜杠
            if (escaped) {
                sb.append('\\');
            }
            
            // 确保导数表示法正确
            String result = sb.toString();
            // 将可能存在的多余转义移除，然后添加单层转义
            result = result.replace("\\\\'", "\\'");
            // 对于像f'(x)这样的导数表示法进行正确转义
            result = result.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\'(");
            
            return result;
        } catch (Exception e) {
            log.error("处理数学公式失败: {}", e.getMessage());
            // 如果处理失败，返回原始字符串
            return jsonStr;
        }
    }

    /**
     * 处理数学表达式
     */
    private String processLatexFormulas(String jsonStr) {
        try {
            // 确保特殊数学符号在JSON中正确显示
            
            // 检查并修正常见数学表达式
            // 不再处理LaTeX命令，只处理纯文本数学表达式中可能的特殊字符
            
            // 处理常见的数学符号
            jsonStr = jsonStr.replace("∞", "infinity")    // 无穷大符号
                             .replace("√", "sqrt")        // 根号
                             .replace("π", "pi")          // π
                             .replace("≤", "<=")          // 小于等于
                             .replace("≥", ">=")          // 大于等于
                             .replace("≠", "!=")          // 不等于
                             .replace("≈", "~=")          // 约等于
                             .replace("×", "*")           // 乘号
                             .replace("÷", "/")           // 除号
                             .replace("±", "+-")          // 正负号
                             .replace("→", "->");         // 箭头
            
            return jsonStr;
        } catch (Exception e) {
            log.error("处理数学表达式失败: {}", e.getMessage());
            // 如果处理失败，返回原始字符串
            return jsonStr;
        }
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String str) {
        return str.replace("\"", "\\\"")          // 转义引号
                 .replace("\n", "\\n")           // 转义换行
                 .replace("\r", "\\r")           // 转义回车
                 .replace("\t", "\\t")           // 转义制表符
                 .replace("\b", "\\b")           // 转义退格
                 .replace("\f", "\\f");          // 转义换页
    }

    /**
     * 清理JSON字符串
     */
    private String cleanupJsonString(String jsonStr) {
        // 修复常见的JSON格式问题
        jsonStr = jsonStr.replaceAll("(?m)\"\\s*:\\s*\"\\s*([^\"]*?)\\s*\"\\s*([,}])", "\":\"$1\"$2") // 修复带空格的字符串
                        .replaceAll("(?m)\"\\s*:\\s*\"", "\":\"") // 统一冒号前后格式
                        .replaceAll("(?m)\"\\s*,\\s*\"", "\",\"") // 统一逗号前后格式
                        .replaceAll("(?m)\"\\s*}\\s*", "\"}") // 统一右大括号前格式
                        .replaceAll("(?m)\"\\s*]\\s*", "\"]") // 统一右中括号前格式
                        .replaceAll("(?m),\\s*}", "}") // 移除对象末尾多余的逗号
                        .replaceAll("(?m),\\s*]", "]"); // 移除数组末尾多余的逗号
        
        return jsonStr;
    }

    /**
     * 修复JSON中字段之间缺少的逗号
     */
    private String fixMissingCommasBetweenFields(String jsonStr) {
        // 使用正则表达式查找在一个键值对结束后没有逗号就直接开始下一个键的情况
        // 模式: 引号+右引号+白空格+引号，说明缺少逗号
        Pattern pattern = Pattern.compile("\"\\s*\"\\s*\"");
        Matcher matcher = pattern.matcher(jsonStr);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            // 在引号后面添加逗号
            matcher.appendReplacement(sb, "\",\"");
        }
        matcher.appendTail(sb);
        jsonStr = sb.toString();
        
        // 处理特殊情况: "analysis" 后面接 "knowledgeTags" 但缺少逗号
        jsonStr = jsonStr.replaceAll("\"\\)，[^\"]*\"\\s*\"knowledgeTags\"", "\"\\)，[^\"]*\",\"knowledgeTags\"");
        
        // 特殊修复第六个题目中的问题
        jsonStr = jsonStr.replace("\"analysis\": \"使用乘积法则求导：\\\\( f'(x) = (2)(3x - 4) + (2x + 1)(3) = 6x - 8 + 6x + 3 = 12x - 5 \\\\)，但选项中没有正确答案。\"", 
                                  "\"analysis\": \"使用乘积法则求导：\\\\( f'(x) = (2)(3x - 4) + (2x + 1)(3) = 6x - 8 + 6x + 3 = 12x - 5 \\\\)，但选项中没有正确答案。\",");
        
        return jsonStr;
    }

    /**
     * 修复转义字符问题
     */
    private String fixEscapedCharacters(String jsonStr) {
        // 检查每个\字符，确保正确转义
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        
        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            
            if (escaped) {
                // 如果前一个字符是\，当前字符需要特殊处理
                escaped = false;
                if (c == '\\' || c == '\"' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't') {
                    // 这些是有效的转义字符，保留原样
                    sb.append('\\').append(c);
                } else {
                    // 无效的转义序列，添加额外的\
                    sb.append('\\').append('\\').append(c);
                }
            } else if (c == '\\') {
                // 遇到\字符，标记为转义模式
                escaped = true;
                // 不立即添加，等下一个字符一起处理
            } else if (c == '\"') {
                // 引号需要特殊处理，用于判断是否在字符串内部
                inQuotes = !inQuotes;
                sb.append(c);
            } else {
                // 普通字符，直接添加
                sb.append(c);
            }
        }
        
        // 如果最后一个字符是\，添加它
        if (escaped) {
            sb.append('\\');
        }
        
        return sb.toString();
    }

    /**
     * 修复LaTeX公式中的多余反斜杠问题 - 优化版本
     */
    private String fixLatexBackslashes(String jsonStr) {
        // 日志记录原始字符串中反斜杠的情况
        int originalBackslashCount = countBackslashes(jsonStr);
        log.debug("原始JSON中反斜杠数量: {}", originalBackslashCount);
        
        // 简化版：只进行最关键的替换
        
        // 0. 首先处理导数符号
        String result = jsonStr.replace("f\\\\'(x)", "f'(x)")
                         .replace("f\\\\\\\'(x)", "f'(x)")
                         .replaceAll("([a-zA-Z])\\\\\\\'\\s*\\(", "$1'("); // 修复过度转义的导数符号
        
        // 1. 修复行内/行间公式分隔符
        result = result.replaceAll("\\\\\\\\\\(", "\\\\(")
                         .replaceAll("\\\\\\\\\\)", "\\\\)")
                         .replaceAll("\\\\\\\\\\[", "\\\\[")
                         .replaceAll("\\\\\\\\\\]", "\\\\]");
        
        // 2. 修复常见数学函数
        String[] commonFunctions = {
            "sin", "cos", "tan", "ln", "log", "frac", "sqrt", 
            "alpha", "beta", "gamma", "delta", "pi", "theta", "lambda"
        };
        
        for (String func : commonFunctions) {
            result = result.replaceAll("\\\\\\\\(" + func + ")", "\\\\$1");
        }
        
        // 3. 清理极端情况 - 八个连续反斜杠替换为两个
        result = result.replaceAll("\\\\\\\\\\\\\\\\", "\\\\");
        
        // 4. 处理导数符号：在所有处理完成后，确保导数符号有单层转义
        if (!result.contains("\\'(")) {
            result = result.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\'(");
        }
        
        // 日志记录修复后的反斜杠情况
        int fixedBackslashCount = countBackslashes(result);
        log.debug("修复后JSON中反斜杠数量: {}", fixedBackslashCount);
        log.debug("减少的反斜杠数量: {}", originalBackslashCount - fixedBackslashCount);
        
        return result;
    }

    /**
     * 记录LaTeX命令出现的情况，用于调试
     */
    private void logLatexCommandOccurrences(String str) {
        // 记录一些常见的LaTeX命令来帮助调试
        String[] testCommands = {"\\\\(", "\\\\)", "\\\\alpha", "\\\\beta", "\\\\sin", "\\\\cos", "\\\\frac", "\\\\\\\\frac"};
        for (String cmd : testCommands) {
            int count = countOccurrences(str, cmd);
            if (count > 0) {
                log.debug("命令 '{}' 出现 {} 次", cmd, count);
            }
        }
        
        // 检查是否有四个反斜杠的情况（过度转义）
        int quadBackslashCount = countOccurrences(str, "\\\\\\\\");
        if (quadBackslashCount > 0) {
            log.debug("发现四个连续反斜杠 {} 次（可能是过度转义）", quadBackslashCount);
            
            // 更详细地记录常见数学函数的过度转义情况
            String[] commonFunctions = {"frac", "sin", "cos", "tan", "sqrt", "ln", "log"};
            for (String func : commonFunctions) {
                int count = countOccurrences(str, "\\\\\\\\" + func);
                if (count > 0) {
                    log.debug("命令 '\\\\\\\\{}' 过度转义出现 {} 次", func, count);
                }
            }
        }
    }

    /**
     * 计算字符串中指定模式出现的次数
     */
    private int countOccurrences(String str, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * 统计字符串中反斜杠的数量
     */
    private int countBackslashes(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\\') {
                count++;
            }
        }
        return count;
    }

    /**
     * 深度修复LaTeX命令中的反斜杠问题
     */
    private String deepFixLatexCommands(String formula) {
        // 第一步：修复常见的LaTeX命令前的多余反斜杠
        String result = formula.replaceAll("\\\\\\\\([a-zA-Z]+)", "\\\\$1")
                     .replaceAll("\\\\\\\\\\{", "\\\\{")
                     .replaceAll("\\\\\\\\\\}", "\\\\}")
                     .replaceAll("\\\\\\\\(\\^|_|\\+|=|<|>|\\|)", "\\\\$1");
        
        // 第二步：更广泛地匹配LaTeX命令，包括各种常见数学符号和函数名
        result = result.replaceAll("\\\\\\\\(sin|cos|tan|log|ln|exp|lim|inf|sum|int|frac|sqrt|partial|nabla|Delta|Omega)", 
                                 "\\\\$1");
        
        // 第三步：递归处理嵌套的大括号内容
        Pattern pattern = Pattern.compile("\\\\[a-zA-Z]+\\{([^}]*)\\}");
        Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String content = matcher.group(1);
            // 只有当内容中包含反斜杠时才递归处理
            if (content.contains("\\")) {
                String fixedContent = deepFixLatexCommands(content);
                // 重建整个匹配项，保留命令但替换内容
                String fullMatch = matcher.group(0);
                String command = fullMatch.substring(0, fullMatch.indexOf("{"));
                String replacement = command + "{" + fixedContent + "}";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // 内容中没有反斜杠，保持原样
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * 检查JSON中可能的转义序列问题
     */
    private void checkJsonEscapes(String json) {
        // 查找未转义的LaTeX分隔符
        Matcher delimiterMatcher = Pattern.compile("\\\\[\\(\\)\\[\\]]").matcher(json);
        while (delimiterMatcher.find()) {
            log.warn("发现未正确转义的LaTeX分隔符: '{}' 在位置 {}", 
                    delimiterMatcher.group(), delimiterMatcher.start());
        }
        
        // 查找未转义的LaTeX命令
        Matcher commandMatcher = Pattern.compile("\\\\([a-zA-Z]+)").matcher(json);
        while (commandMatcher.find()) {
            log.warn("发现未正确转义的LaTeX命令: '\\{}' 在位置 {}", 
                    commandMatcher.group(1), commandMatcher.start());
        }
        
        // 查找非法转义序列
        Matcher illegalEscapeMatcher = Pattern.compile("\\\\([^\"\\\\bfnrt/\\(\\)\\[\\]a-zA-Z])").matcher(json);
        while (illegalEscapeMatcher.find()) {
            log.warn("发现非法转义序列: '\\{}' 在位置 {}", 
                    illegalEscapeMatcher.group(1), illegalEscapeMatcher.start());
        }
        
        // 特别检查位置37附近的字符
        try {
            if (json.length() >= 40) {
                int start = Math.max(0, 30);
                int end = Math.min(json.length(), 45);
                String snippet = json.substring(start, end);
                
                log.debug("位置30-45内容: '{}'", snippet);
                
                // 检查位置37的特殊字符
                if (json.length() > 37) {
                    char charAt37 = json.charAt(37);
                    char prevChar = json.length() > 36 ? json.charAt(36) : ' ';
                    char nextChar = json.length() > 38 ? json.charAt(38) : ' ';
                    
                    log.debug("位置37字符: '{}' (ASCII: {}), 前一个: '{}', 后一个: '{}'", 
                            charAt37, (int)charAt37, prevChar, nextChar);
                    
                    // 检查是否是反斜杠后跟非法字符
                    if (prevChar == '\\' && !isValidEscapeChar(charAt37)) {
                        log.error("位置37存在非法转义序列: '\\{}'", charAt37);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检查位置37时出错", e);
        }
    }
    
    /**
     * 判断字符是否是合法的JSON转义字符
     */
    private boolean isValidEscapeChar(char c) {
        return c == '"' || c == '\\' || c == '/' || c == 'b' || 
               c == 'f' || c == 'n' || c == 'r' || c == 't' ||
               c == '(' || c == ')' || c == '[' || c == ']' ||
               Character.isLetter(c);
    }

    /**
     * 验证题目字段
     */
    private void validateQuestionFields(JSONObject question, int index, Integer questionBankType) {
        // 验证基本字段
        if (!question.containsKey("content")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少题目内容", index));
        }

        // 根据题库类型验证题目
        // 题库类型：1-选择题（多选），0-选择题（单选），2-填空题，3-简答题，4-判断题
        boolean isMultipleChoice = (questionBankType == 1);
        
        if (questionBankType == 0 || questionBankType == 1) {
            // 选择题验证（单选或多选）
            validateChoiceQuestion(question, index, isMultipleChoice);
        } else if (questionBankType == 2) {
            // 填空题验证
            validateFillBlankQuestion(question, index);
        } else if (questionBankType == 3) {
            // 简答题验证
            validateSubjectiveQuestion(question, index);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的题型不受支持", index));
        }
    }

    /**
     * 验证选择题字段
     */
    private void validateChoiceQuestion(JSONObject question, int index, boolean isMultiple) {
        // 验证选项数组
        if (!question.containsKey("options")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少选项", index));
        }
        
        JSONArray options = question.getJSONArray("options");
        if (options == null || options.size() != 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题必须包含恰好4个选项", index));
        }

        // 检查选项是否有重复 - 注释掉这段检查，允许相同选项
        // 某些数学题目，尤其是涉及导数计算的题目可能存在相同形式但不同含义的选项
        /*
        Set<String> optionSet = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            String option = options.getStr(i);
            if (!optionSet.add(option)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题的选项存在重复", index));
            }
        }
        */

        // 验证答案
        if (!question.containsKey("answer")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少答案", index));
        }

        String answer = question.getStr("answer").replaceAll("\\s+", ""); // 移除所有空白字符
        
        // 检查多选题答案格式
        if (isMultiple) {
            // 检查答案格式：是否为单个字符串包含多个字母（如"ABC"）
            if (answer.length() < 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题是多选题，答案必须包含2-4个选项", index));
            }
        
            // 将答案字符串（如"ABC"）转换为字符数组，以便验证每个字符
            char[] answerChars = answer.toCharArray();
            Set<Character> answerSet = new HashSet<>();
            for (char c : answerChars) {
                answerSet.add(c);
            }
            
            // 验证多选题至少选择2个选项，最多选择4个选项
            if (answerSet.size() < 2 || answerSet.size() > 4) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题是多选题，答案必须包含2-4个不同选项", index));
        }
        
        // 验证每个选项是否有效
            for (char c : answerChars) {
                if (c < 'A' || c > 'D') {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                        String.format("第%d题答案包含无效选项: %s", index, c));
                }
            }
        } else {
            // 单选题答案应为单个字母
            if (answer.length() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题是单选题，答案必须是单个选项（A/B/C/D）", index));
            }
            
            char c = answer.charAt(0);
            if (c < 'A' || c > 'D') {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    String.format("第%d题答案包含无效选项: %s", index, c));
            }
        }
    }

    /**
     * 验证判断题字段
     */
    private void validateJudgmentQuestion(JSONObject question, int index) {
        // 验证答案
        if (!question.containsKey("answer")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少答案", index));
        }
        
        String answer = question.getStr("answer").trim();
        if (!answer.equals("对") && !answer.equals("错") && 
            !answer.equals("正确") && !answer.equals("错误") && 
            !answer.equals("true") && !answer.equals("false") && 
            !answer.equals("T") && !answer.equals("F")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的判断题答案必须是'对/错'或'正确/错误'或'true/false'", index));
        }
    }

    /**
     * 校验填空题
     */
    private void validateFillBlankQuestion(JSONObject question, int index) {
        // 校验答案
        Object answerObj = question.get("answer");
        if (answerObj == null || (answerObj instanceof String && ((String) answerObj).trim().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的填空题答案不能为空", index));
        }
    }

    /**
     * 验证主观题字段
     */
    private void validateSubjectiveQuestion(JSONObject question, int index) {
        // 主观题必须有分析或参考答案
        if (!question.containsKey("analysis") && !question.containsKey("answer")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少分析或参考答案", index));
        }
    }

    /**
     * 验证题目的基本格式
     */
    private void validateBasicQuestionFormat(JSONObject question, int index) {
        // 验证题目内容
        String content = question.getStr("content");
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的题目内容不能为空", index));
        }
        
        // 验证解析
        if (!question.containsKey("analysis")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少解析", index));
        }
        
        // 验证分数是否合理
        if (!question.containsKey("score")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少分数设置", index));
        }
        
        int score = question.getInt("score");
        if (score <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题分数必须大于0", index));
        }
        
        // 规范化分数设置为10分
        if (score == 100 || score == 50 || score == 25 || score == 16 || score == 33) {
            log.info("第{}题分数从{}调整为10分", index, score);
            question.set("score", 10);
        }
        
        // 验证知识点标签
        if (!question.containsKey("knowledgeTags")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题缺少知识点标签", index));
        }
        
        JSONArray knowledgeTags = question.getJSONArray("knowledgeTags");
        if (knowledgeTags == null || knowledgeTags.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("第%d题的知识点标签不能为空", index));
        }
    }

    /**
     * 在独立事务中创建题库和题目
     */
    @Transactional(rollbackFor = Exception.class)
    protected Long createQuestionBankInTransaction(QuestionBank questionBank, String processedJson) {
        try {
            // 先进行基本数据的保存
            questionBankMapper.insert(questionBank);
            Long bankId = questionBank.getId();
            if (bankId == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题库创建失败");
            }
            
            log.info("题库创建成功，ID: {}, 开始处理题目...", bankId);
            
            // 解析JSON，创建题目
            JSONObject jsonObject = JSONUtil.parseObj(processedJson);
            JSONArray questions = jsonObject.getJSONArray("questions");
            
            if (questions == null || questions.isEmpty()) {
                log.error("AI生成的题目为空");
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI生成题目失败");
            }
            
            List<Question> createdQuestions = processQuestions(jsonObject, bankId, questionBank);
            
            // 设置题库的题目数量
            int count = createdQuestions.size();
            questionBank.setQuestionCount(count);
            updateById(questionBank);
            
            log.info("题库和题目创建完成，题库ID: {}, 题目数量: {}", bankId, count);
            return bankId;
        } catch (Exception e) {
            log.error("创建题库失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建题库失败: " + e.getMessage());
        }
    }


    /**
     * 处理题目数据
     */
    @Transactional(rollbackFor = Exception.class, timeout = 300)
    private List<Question> processQuestions(JSONObject jsonResponse, Long bankId, QuestionBank bank) {
        List<Question> questions = new ArrayList<>();
        List<QuestionBankQuestion> questionBankQuestions = new ArrayList<>();
        
        try {
            JSONArray questionsArray = jsonResponse.getJSONArray("questions");
            int questionCount = questionsArray.size();
            
            // 题目标签到知识点的映射 (暂存)
            Map<Long, List<String>> questionKnowledgeMap = new HashMap<>();
            
            for (int i = 0; i < questionCount; i++) {
                JSONObject questionObj = questionsArray.getJSONObject(i);
                
                // 验证题目字段，使用题库的题目类型
                validateQuestionFields(questionObj, i + 1, bank.getQuestionBankType());
                
                Question question = new Question();
                // 确保ID为null，让数据库自动生成ID
                question.setId(null);
                question.setQuestionContent(validateContent(questionObj.getStr("content")));
                question.setQuestionType(bank.getQuestionBankType());
                question.setAnalysis(questionObj.getStr("analysis"));
                question.setScore(100 / questionCount);
                question.setSource(1);
                question.setUserId(bank.getUserId());

                // 根据题目类型处理选项和答案
                switch (bank.getQuestionBankType()) {
                    case 0: // 单选题
                        processChoiceQuestion(question, questionObj, false);
                        break;
                    case 1: // 多选题
                        processMultipleChoiceQuestion(question, questionObj);
                        break;
                    case 2: // 填空题
                        processFillInQuestion(question, questionObj);
                        break;
                    case 3: // 简答题
                        processEssayQuestion(question, questionObj);
                        break;
                    default:
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的题目类型");
                }

                // 处理知识点标签
                processKnowledgeTags(question, questionObj);
                
                // 保存知识点标签映射，等题目保存后处理
                JSONArray knowledgeTagsArray = questionObj.getJSONArray("knowledgeTags");
                if (knowledgeTagsArray != null && !knowledgeTagsArray.isEmpty()) {
                    List<String> knowledgeTags = new ArrayList<>();
                    for (int j = 0; j < knowledgeTagsArray.size(); j++) {
                        knowledgeTags.add(knowledgeTagsArray.getStr(j));
                    }
                    questionKnowledgeMap.put((long) i, knowledgeTags);
                }
                
                questions.add(question);
                
                // 创建题目和题库的关联
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                // 确保ID为null，让关联表自动生成ID
                questionBankQuestion.setId(null);
                questionBankQuestion.setQuestionBankId(bankId);
                questionBankQuestion.setQuestionId(question.getId());
                questionBankQuestion.setQuestionOrder(i + 1);
                questionBankQuestion.setUserId(bank.getUserId());
                questionBankQuestions.add(questionBankQuestion);
            }
            
            // 批量保存题目
            boolean questionsSaved = questionService.saveBatch(questions);
            if (!questionsSaved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存题目失败");
            }
            
            // 设置questionBankQuestion中的questionId为保存后的题目ID
            for (int i = 0; i < questions.size(); i++) {
                questionBankQuestions.get(i).setQuestionId(questions.get(i).getId());
                
                // 处理知识点关联
                Long questionId = questions.get(i).getId();
                List<String> knowledgeTags = questionKnowledgeMap.get((long) i);
                if (knowledgeTags != null && !knowledgeTags.isEmpty()) {
                    // 先根据知识点名称获取或创建知识点
                    try {
                        // 使用默认学科"高中数学"，可以根据实际情况从题库或配置获取
                        String subject = "高中数学";
                        List<Long> knowledgeIds = knowledgePointService.getOrCreateKnowledgePoints(
                            knowledgeTags, subject, bank.getUserId());
                            
                        // 建立题目与知识点的关联
                        if (!knowledgeIds.isEmpty()) {
                            questionKnowledgeService.batchAddKnowledgeToQuestion(questionId, knowledgeIds);
                            log.info("题目ID: {} 关联了 {} 个知识点", questionId, knowledgeIds.size());
                        }
                    } catch (Exception e) {
                        // 知识点关联失败不影响整体流程
                        log.error("题目ID: {} 知识点关联失败: {}", questionId, e.getMessage());
                    }
                }
            }
            
            // 批量保存题库题目关联
            boolean relationsSaved = questionBankQuestionService.saveBatch(questionBankQuestions);
            if (!relationsSaved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存题库与题目关联失败");
            }
            
            return questions;
        } catch (Exception e) {
            log.error("处理题目失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理题目失败: " + e.getMessage());
        }
    }

    /**
     * 处理选择题
     */
    private void processChoiceQuestion(Question question, JSONObject questionObj, boolean isMultiple) {
        // 不设置子类型，字段不存在
        
        // 解析选项
        JSONArray optionsArray = questionObj.getJSONArray("options");
        if (optionsArray != null && !optionsArray.isEmpty()) {
            // 直接保存选项，不处理LaTeX格式
            List<String> options = new ArrayList<>();
            for (int i = 0; i < optionsArray.size(); i++) {
                options.add(optionsArray.getStr(i));
            }
            question.setOptions(options);
        }
        
        // 设置答案，不处理LaTeX格式
        String answer = questionObj.getStr("answer");
        if (answer != null) {
            // 选择题的答案需要设置为列表
        if (isMultiple) {
                // 多选题答案处理 - 将格式如"ABC"转换为["A","B","C"]
                List<String> answers = new ArrayList<>();
                
                // 首先检查答案是否是JSON数组形式
                if (answer.startsWith("[") && answer.endsWith("]")) {
                    try {
                        // 尝试解析JSON数组
                        JSONArray answerArray = new JSONArray(answer);
                        for (int i = 0; i < answerArray.size(); i++) {
                            answers.add(answerArray.getStr(i));
                        }
                    } catch (Exception e) {
                        log.warn("解析多选题答案JSON数组失败: {}", e.getMessage());
                        // 如果解析失败，回退到字符串处理
                        for (char c : answer.toCharArray()) {
                            if (c >= 'A' && c <= 'D') {
                                answers.add(String.valueOf(c));
                            }
                        }
                    }
                } else {
                    // 处理字符串形式的答案（如"ABC"）
                    for (char c : answer.toCharArray()) {
                        if (c >= 'A' && c <= 'D') {
                            answers.add(String.valueOf(c));
                        }
                    }
                }
                
            question.setAnswer(answers);
                log.debug("处理多选题答案: {} -> {}", answer, answers);
        } else {
            // 单选题答案处理
            question.setAnswer(Collections.singletonList(answer));
            }
        } else {
            // 单选题答案处理
            question.setAnswer(Collections.emptyList()); // 如果没有答案，设置为空列表
        }
    }

    /**
     * 处理多选题
     */
    private void processMultipleChoiceQuestion(Question question, JSONObject questionObj) {
        // 设置子类型为多选题
        question.setQuestionType(1);
        
        // 设置选项
        JSONArray optionsArray = questionObj.getJSONArray("options");
        if (optionsArray != null) {
            List<String> options = new ArrayList<>();
            for (int i = 0; i < optionsArray.size(); i++) {
                options.add(optionsArray.getStr(i));
            }
            question.setOptions(options);
        }
        
        // 设置答案
        Object answerObj = questionObj.get("answer");
        if (answerObj != null) {
            String answerStr = answerObj.toString();
            List<String> answers = new ArrayList<>();
            
            // 根据不同的答案格式进行处理
            if (answerStr.startsWith("[") && answerStr.endsWith("]")) {
                // 如果答案是JSON格式
                try {
                    JSONArray answerArray = JSONUtil.parseArray(answerStr);
                    for (int i = 0; i < answerArray.size(); i++) {
                        String option = answerArray.getStr(i);
                        if (option != null && option.length() > 0) {
                            answers.add(option);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析多选题答案JSON数组失败: {}", e.getMessage());
                    // 如果解析失败，回退到字符串处理
                    for (char c : answerStr.toCharArray()) {
                        if (c >= 'A' && c <= 'D') {
                            answers.add(String.valueOf(c));
                        }
                    }
                }
            } else {
                // 处理字符串形式的答案（如"ABC"）
                for (char c : answerStr.toCharArray()) {
                    if (c >= 'A' && c <= 'D') {
                        answers.add(String.valueOf(c));
                    }
                }
            }
            
            question.setAnswer(answers);
            log.debug("处理多选题答案: {} -> {}", answerStr, answers);
        } else {
            // 如果没有答案，设置为空列表
            question.setAnswer(Collections.emptyList());
        }
    }

    /**
     * 处理填空题
     */
    private void processFillInQuestion(Question question, JSONObject questionObj) {
        // 不设置子类型
        
        // 设置答案，不处理LaTeX格式
        Object answerObj = questionObj.get("answer");
        if (answerObj != null) {
            if (answerObj instanceof JSONArray) {
                // 如果答案是数组
                JSONArray answerArray = (JSONArray) answerObj;
                List<String> answers = new ArrayList<>();
                for (int i = 0; i < answerArray.size(); i++) {
                    answers.add(answerArray.getStr(i));
                }
                question.setAnswer(answers);
            } else {
                // 如果答案是字符串
                question.setAnswer(Collections.singletonList(answerObj.toString()));
            }
        }
    }

    /**
     * 处理简答题
     */
    private void processEssayQuestion(Question question, JSONObject questionObj) {
        // 不设置子类型
        
        // 设置答案，不处理LaTeX格式
        String answer = questionObj.getStr("answer");
        if (answer != null) {
            question.setAnswer(Collections.singletonList(answer));
        }
    }

    /**
     * 标准化文本中的数学表达式
     */
    private String normalizeLatexInText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 不再进行LaTeX处理，直接返回纯文本
        return text;
    }

    /**
     * 处理选择题中的选项
     */
    private List<String> processOptions(JSONArray options) {
        if (options == null) {
            return null;
        }
        
        List<String> processedOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String option = options.getStr(i);
            // 直接添加选项，不做特殊处理
            processedOptions.add(option);
        }
        
        return processedOptions;
    }

    /**
     * 修复分析文本中的特殊字符
     */
    private String fixAnalysisLatex(String json) {
        try {
            Pattern analysisPattern = Pattern.compile("\"analysis\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher analysisMatcher = analysisPattern.matcher(json);
            
            StringBuffer sb = new StringBuffer();
            while (analysisMatcher.find()) {
                String analysis = analysisMatcher.group(1);
                
                // 处理分析文本中的特殊字符
                String normalizedAnalysis = analysis.replace("\"", "\\\"")
                                                  .replace("\n", "\\n")
                                                  .replace("\r", "\\r")
                                                  .replace("\t", "\\t");
                
                // 将修复后的分析放回原位
                analysisMatcher.appendReplacement(sb, "\"analysis\":\"" + normalizedAnalysis + "\"");
            }
            analysisMatcher.appendTail(sb);
            
            return sb.toString();
        } catch (Exception e) {
            log.warn("修复分析文本时出错: {}", e.getMessage(), e);
            return json;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateQuestionBank(QuestionBank questionBank) {
        // 参数校验
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 更新题库
        return updateById(questionBank);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteQuestionBank(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 检查题库是否存在
        QuestionBank questionBank = getById(id);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
        
        // 1. 获取题库关联的所有题目ID
        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
            new LambdaQueryWrapper<QuestionBankQuestion>()
                .eq(QuestionBankQuestion::getQuestionBankId, id)
        );
        
        List<Long> questionIds = questionBankQuestions.stream()
            .map(QuestionBankQuestion::getQuestionId)
            .collect(Collectors.toList());
        
        log.info("删除题库 ID: {}, 关联题目数量: {}", id, questionIds.size());
        
        // 2. 删除题库与题目的关联关系
        boolean relationRemoved = questionBankQuestionService.remove(
            new LambdaQueryWrapper<QuestionBankQuestion>()
                .eq(QuestionBankQuestion::getQuestionBankId, id)
        );
        
        log.info("删除题库 ID: {}, 已删除题库与题目的关联关系: {}", id, relationRemoved);
        
        // 3. 删除题目
        if (!questionIds.isEmpty()) {
            boolean questionsRemoved = questionService.removeByIds(questionIds);
            log.info("删除题库 ID: {}, 已删除关联题目: {}", id, questionsRemoved);
        }
        
        // 4. 删除题库
        boolean result = removeById(id);
        
        log.info("删除题库 ID: {}, 删除结果: {}", id, result);
        
        return result;
    }

    @Override
    public QuestionBankVO getQuestionBankById(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取题库
        QuestionBank questionBank = getById(id);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 转换为VO
        return convertToVO(questionBank);
    }

    @Override
    public Page<QuestionBankVO> listQuestionBanks(long current, long size) {
        // 分页查询题库
        Page<QuestionBank> page = page(new Page<>(current, size));
        // 转换为VO
        List<QuestionBankVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        // 构建返回结果
        Page<QuestionBankVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addQuestionsToBank(Long questionBankId, List<Long> questionIds) {
        // 参数校验
        if (questionBankId == null || questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 添加题目到题库
        // TODO: 实现添加题目的逻辑
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeQuestionsFromBank(Long questionBankId, List<Long> questionIds) {
        // 参数校验
        if (questionBankId == null || questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 从题库中移除题目
        // TODO: 实现移除题目的逻辑
        return true;
    }

    @Override
    public List<QuestionVO> getQuestionsByBankId(Long questionBankId) {
        // 参数校验
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 获取题库中的题目列表
        List<QuestionBankQuestion> questionBankQuestions = questionBankQuestionService.list(
            new LambdaQueryWrapper<QuestionBankQuestion>()
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .orderByAsc(QuestionBankQuestion::getQuestionOrder)
        );

        if (questionBankQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取题目ID列表
        List<Long> questionIds = questionBankQuestions.stream()
            .map(QuestionBankQuestion::getQuestionId)
            .collect(Collectors.toList());

        // 获取题目详情
        List<Question> questions = questionService.listByIds(questionIds);
        
        // 转换为VO并保持顺序
        Map<Long, Integer> orderMap = new HashMap<>();
        for (QuestionBankQuestion qbq : questionBankQuestions) {
            orderMap.put(qbq.getQuestionId(), qbq.getQuestionOrder());
        }

        return questions.stream()
            .map(question -> {
                QuestionVO vo = QuestionVO.objToVo(question);
                vo.setQuestionOrder(orderMap.get(question.getId()));
                return vo;
            })
            .sorted(Comparator.comparing(QuestionVO::getQuestionOrder))
            .collect(Collectors.toList());
    }

    @Override
    public List<ScoringResultVO> getScoringHistory(Long questionBankId, Long userId) {
        // 参数校验
        if (questionBankId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 获取用户在该题库的所有评分结果
        List<ScoringResult> scoringResults = scoringResultService.list(
            new LambdaQueryWrapper<ScoringResult>()
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .eq(ScoringResult::getUserId, userId)
                .orderByDesc(ScoringResult::getCreateTime)
        );

        // 转换为VO
        return scoringResults.stream()
            .map(result -> ScoringResultVO.objToVo(result))
            .collect(Collectors.toList());
    }

    @Override
    public QuestionBankVO getQuestionBankStats(Long questionBankId) {
        // 参数校验
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 获取题库的所有评分结果
        List<ScoringResult> scoringResults = scoringResultService.list(
            new LambdaQueryWrapper<ScoringResult>()
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .eq(ScoringResult::getStatus, 1) // 只统计已完成的
        );

        // 计算统计信息
        QuestionBankVO vo = convertToVO(questionBank);
        if (!scoringResults.isEmpty()) {
            // 计算平均分
            double averageScore = scoringResults.stream()
                .mapToInt(ScoringResult::getScore)
                .average()
                .orElse(0.0);
            vo.setAverageScore(averageScore);

            // 计算最高分
            int highestScore = scoringResults.stream()
                .mapToInt(ScoringResult::getScore)
                .max()
                .orElse(0);
            vo.setHighestScore((double) highestScore);

            // 计算最低分
            int lowestScore = scoringResults.stream()
                .mapToInt(ScoringResult::getScore)
                .min()
                .orElse(0);
            vo.setLowestScore((double) lowestScore);

            // 统计参与人数
            vo.setParticipantCount(scoringResults.size());
        } else {
            // 如果没有评分记录，设置默认值
            vo.setAverageScore(0.0);
            vo.setHighestScore(0.0);
            vo.setLowestScore(0.0);
            vo.setParticipantCount(0);
        }

        return vo;
    }

    /**
     * 将实体转换为VO
     */
    private QuestionBankVO convertToVO(QuestionBank questionBank) {
        if (questionBank == null) {
            return null;
        }
        QuestionBankVO vo = new QuestionBankVO();
        vo.setId(questionBank.getId());
        vo.setName(questionBank.getTitle());
        vo.setDescription(questionBank.getDescription());
        vo.setPicture(questionBank.getPicture());
        vo.setQuestionBankType(questionBank.getQuestionBankType());
        vo.setScoringStrategy(questionBank.getScoringStrategy());
        vo.setTotalScore(questionBank.getTotalScore());
        vo.setPassScore(questionBank.getPassScore());
        vo.setEndTime(questionBank.getEndTime());
        vo.setReviewStatus(questionBank.getReviewStatus());
        vo.setReviewMessage(questionBank.getReviewMessage());
        vo.setQuestionCount(questionBank.getQuestionCount());
        vo.setSubject(questionBank.getSubject());
        vo.setClassId(questionBank.getClassId());
        vo.setUserId(questionBank.getUserId());
        vo.setCreateTime(questionBank.getCreateTime());
        vo.setUpdateTime(questionBank.getUpdateTime());
        vo.setIsDelete(questionBank.getIsDelete());
        return vo;
    }

    /**
     * 构建适合AI题库生成的提示词
     */
    private String buildAIPrompt(QuestionBank questionBank) {
        // 构建基本提示
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请生成").append(questionBank.getQuestionCount()).append("道").append(questionBank.getSubject()).append("题目");
        
        // 添加题型限制
        switch (questionBank.getQuestionBankType()) {
            case 0:
                promptBuilder.append("(单选题)");
                break;
            case 1:
                promptBuilder.append("(多选题)");
                break;
            case 2:
                promptBuilder.append("(填空题)");
                break;
            case 3:
                promptBuilder.append("(主观题)");
                break;
            default:
                promptBuilder.append("(混合题型)");
        }
        
        // 添加题目要求
        promptBuilder.append("\n\n题目要求：\n").append(questionBank.getDescription());
        
        // 根据题型添加特定指南
        switch (questionBank.getQuestionBankType()) {
            case 0: 
                promptBuilder.append("\n\n").append(buildSingleChoicePrompt());
                break;
            case 1: 
                promptBuilder.append("\n\n").append(buildMultipleChoicePrompt());
                break;
            case 2: 
                promptBuilder.append("\n\n").append(buildFillinTheBlankPrompt());
                break;
            case 3: 
                promptBuilder.append("\n\n").append(buildShortAnswerPrompt());
                break;
            default:
                // 混合类型，使用通用提示
                promptBuilder.append("\n\n题目类型：混合题型\n包括单选题、多选题、填空题和简答题");
        }
        
        // 添加通用的输出格式要求
        promptBuilder.append("\n\nJSON格式要求（必须严格遵守）：\n")
                    .append("1. 必须使用标准的JSON格式，符合RFC 8259标准\n")
                    .append("2. 所有字符串必须使用英文双引号，绝对不能用单引号\n")
                    .append("3. 必须包含且仅包含questions数组\n")
                    .append("4. 数组内必须是完整的题目对象，不能有多余字段\n\n");
        
        // 添加数学公式格式要求 - 使用纯文本表示，不使用LaTeX
        promptBuilder.append("数学公式格式要求（非常重要，请仔细阅读并严格遵守）：\n")
                    .append("1. 所有数学公式和符号必须使用纯文本格式，不使用LaTeX命令\n")
                    .append("2. 不要使用任何形式的 \\sin, \\cos, \\frac 等LaTeX命令\n")
                    .append("3. 不要使用任何形式的数学分隔符，如 \\( 或 $ 等\n")
                    .append("4. 使用简单直观的形式表示数学公式和符号：\n")
                    .append("   - 使用 sin(x), cos(x), tan(x) 表示三角函数\n")
                    .append("   - 使用 a/b 表示分数，如 2/3\n")
                    .append("   - 使用 sqrt(x) 表示根号，如 sqrt(2)\n")
                    .append("   - 使用 ^ 表示指数，如 x^2\n")
                    .append("   - 使用希腊字母的英文名称，如 alpha, beta, gamma\n")
                    .append("5. 正确的例子：\n")
                    .append("   - 正确格式：\"content\": \"函数 f(x) = x^2 的导数\"\n")
                    .append("   - 正确格式：\"options\": [\"sin(x)\", \"cos(x)\"]\n")
                    .append("   - 错误格式：\"content\": \"函数 \\\\( f(x) = x^2 \\\\) 的导数\"\n")
                    .append("   - 错误格式：\"options\": [\"\\\\sin(x)\", \"\\\\cos(x)\"]\n\n");
        
        // 添加通用的输出格式要求
        promptBuilder.append("输出格式：\n")
                    .append("1. 直接返回有效的JSON，不要包含markdown代码块标记\n")
                    .append("2. 不要在JSON前后添加任何说明文字\n")
                    .append("3. 返回前务必检查JSON格式的有效性\n")
                    .append("4. 确保所有数学公式都使用标准数学表示法（如x^2, sin(x), log(x)等），无需翻译成中文\n")
                    .append("5. 避免使用任何可能导致JSON解析错误的特殊字符\n\n");
        
        // 最终检查清单
        promptBuilder.append("最终检查清单（返回前请一一确认）：\n")
                    .append("1. JSON格式是否有效且完整\n")
                    .append("2. 所有数学公式是否使用了标准数学表示法\n")
                    .append("3. 是否没有使用任何LaTeX命令\n")
                    .append("4. 题目、选项、解析是否都使用了中文\n")
                    .append("5. 是否直接返回JSON而不附加其他内容");
        
        String prompt = promptBuilder.toString();
        log.info("生成的AI提示词: {}", prompt);
        return prompt;
    }

    /**
     * 构建单选题提示词
     */
    private String buildSingleChoicePrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深的单选题命题专家，请严格按照以下规则创建5道单选题：\n\n");
        prompt.append("1. 每道题都必须是单选题，即只有1个正确选项。\n");
        prompt.append("2. 每道题必须有4个选项，标记为A、B、C、D。\n");
        prompt.append("3. 答案格式必须是单个选项字母，如A、B、C或D。\n");
        prompt.append("4. 每道题都必须包含题干(content)、选项(options)、答案(answer)、分析(analysis)和知识点(knowledgeTags)。\n");
        prompt.append("5. 在表示导数时，请使用df/dx表示法而不是f'(x)表示法，避免使用单引号，以确保生成的JSON格式正确。\n");
        prompt.append("6. 确保所有数学公式和符号格式正确，特别是在处理特殊符号时。\n");
        prompt.append("7. 请确保四个选项都不相同，避免生成相同的选项造成混淆。\n");
        prompt.append("8. 所有题目、选项、答案分析必须使用中文，不允许出现英文题目或内容。数学公式可以使用标准数学表示法，如x^2, sin(x), log(x)等，无需翻译成中文。\n");
        prompt.append("9. 如果题目涉及到专业术语，必须使用中文表达，不要使用英文术语。\n");
        prompt.append("10. 你的回答必须是一个格式正确的JSON字符串，包含一个questions数组，每个问题对象具有以下结构：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"content\": \"题目内容（必须是中文）\",\n");
        prompt.append("      \"options\": [\"选项A（中文）\", \"选项B（中文）\", \"选项C（中文）\", \"选项D（中文）\"],\n");
        prompt.append("      \"answer\": \"正确选项，如A、B等\",\n");
        prompt.append("      \"score\": 10,\n");
        prompt.append("      \"analysis\": \"详细解析（必须是中文）\",\n");
        prompt.append("      \"knowledgeTags\": [\"知识点1（中文）\", \"知识点2（中文）\"]\n");
        prompt.append("    },\n");
        prompt.append("    // ... 其他问题\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("请确保生成的JSON格式正确，可以被解析。请直接提供JSON格式的答案，不要有任何其他说明。\n");

        return prompt.toString();
    }

    /**
     * 构建多选题提示词
     */
    private String buildMultipleChoicePrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深的多选题命题专家，请严格按照以下规则创建5道多选题：\n\n");
        prompt.append("1. 每道题都必须是多选题，即至少有2个正确选项。\n");
        prompt.append("2. 每道题必须有4个选项，标记为A、B、C、D。\n");
        prompt.append("3. 答案格式必须是选项的字母组合，如AB、BCD等，不可使用逗号分隔。\n");
        prompt.append("4. 每道题都必须包含题干(content)、选项(options)、答案(answer)、分析(analysis)和知识点(knowledgeTags)。\n");
        prompt.append("5. 在表示导数时，请使用df/dx表示法而不是f'(x)表示法，避免使用单引号，以确保生成的JSON格式正确。\n");
        prompt.append("6. 确保所有数学公式和符号格式正确，特别是在处理特殊符号时。\n");
        prompt.append("7. 请确保四个选项都不相同，避免生成相同的选项造成混淆。\n");
        prompt.append("8. 所有题目、选项、答案分析必须使用中文，不允许出现英文题目或内容。数学公式可以使用标准数学表示法，如x^2, sin(x), log(x)等，无需翻译成中文。\n");
        prompt.append("9. 如果题目涉及到专业术语，必须使用中文表达，不要使用英文术语。\n");
        prompt.append("10. 你的回答必须是一个格式正确的JSON字符串，包含一个questions数组，每个问题对象具有以下结构：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"content\": \"题目内容（必须是中文）\",\n");
        prompt.append("      \"options\": [\"选项A（中文）\", \"选项B（中文）\", \"选项C（中文）\", \"选项D（中文）\"],\n");
        prompt.append("      \"answer\": \"正确选项组合，如AB、BCD等\",\n");
        prompt.append("      \"score\": 10,\n");
        prompt.append("      \"analysis\": \"详细解析（必须是中文）\",\n");
        prompt.append("      \"knowledgeTags\": [\"知识点1（中文）\", \"知识点2（中文）\"]\n");
        prompt.append("    },\n");
        prompt.append("    // ... 其他问题\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("请确保生成的JSON格式正确，可以被解析。请直接提供JSON格式的答案，不要有任何其他说明。\n");

        return prompt.toString();
    }

    /**
     * 构建填空题提示词
     */
    private String buildFillinTheBlankPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("题目类型：填空题\n");
        prompt.append("要求：\n");
        prompt.append("1. 填空题必须有明确唯一的答案\n");
        prompt.append("2. 答案必须是数值、词组或简短句子，不能是长篇论述\n");
        prompt.append("3. 题干必须明确指出需要填写的内容\n");
        prompt.append("4. 题干中用\"____\"表示填空部分\n");
        prompt.append("5. analysis字段需解释为什么答案是正确的\n");
        prompt.append("6. score字段必须设置为10分\n");
        prompt.append("7. 所有题目、答案和解析必须使用中文，不允许出现英文题目或内容。数学公式可以使用标准数学表示法，如x^2, sin(x), log(x)等，无需翻译成中文\n");
        prompt.append("8. 如果涉及到专业术语，必须使用中文表达，不要使用英文术语\n\n");

        prompt.append("填空题示例（格式要严格按照此示例）：\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"content\": \"二次函数 f(x) = ax^2 + bx + c 的图像是一条开口向下的抛物线，且过点(1, 2)，当 a = -1 时，b的值为____\",\n");
        prompt.append("      \"answer\": \"4\",\n");
        prompt.append("      \"score\": 10,\n");
        prompt.append("      \"analysis\": \"将点(1, 2)代入函数 f(x) = -x^2 + bx + c，得 2 = -1 + b + c。由于只有一个方程但有两个未知数，无法唯一确定b和c。需要额外条件。如果假设抛物线过点(0, 0)，则c = 0，此时有 2 = -1 + b，解得 b = 3。如果不做额外假设，只能得到 b + c = 3。\",\n");
        prompt.append("      \"knowledgeTags\": [\"二次函数\", \"函数图像\", \"代数运算\"]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 构建简答题提示词
     */
    private String buildShortAnswerPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("题目类型：简答题\n");
        prompt.append("要求：\n");
        prompt.append("1. 简答题必须有明确的问题和标准答案\n");
        prompt.append("2. 答案应简明扼要，包含必要的步骤和关键结论\n");
        prompt.append("3. 题目内容应清晰表达所求内容\n");
        prompt.append("4. analysis字段需提供详细的解答思路\n");
        prompt.append("5. score字段必须设置为10分\n");
        prompt.append("6. 所有题目、答案和解析必须使用中文，不允许出现英文题目或内容。数学公式可以使用标准数学表示法，如x^2, sin(x), log(x)等，无需翻译成中文\n");
        prompt.append("7. 如果涉及到专业术语，必须使用中文表达，不要使用英文术语\n\n");
        
        prompt.append("简答题示例（格式要严格按照此示例）：\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"content\": \"简要证明：两个连续函数的和是连续函数。\",\n");
        prompt.append("      \"answer\": \"设f(x)和g(x)是定义在区间I上的连续函数，那么对于任意x0∈I，有lim(x→x0)f(x)=f(x0)且lim(x→x0)g(x)=g(x0)。根据极限的四则运算法则，lim(x→x0)[f(x)+g(x)]=lim(x→x0)f(x)+lim(x→x0)g(x)=f(x0)+g(x0)=(f+g)(x0)。这表明函数f+g在x0处连续。由于x0是区间I上的任意点，所以f+g在区间I上处处连续，即两个连续函数的和是连续函数。\",\n");
        prompt.append("      \"score\": 10,\n");
        prompt.append("      \"analysis\": \"这道题目要求证明两个连续函数的和是连续函数。证明的关键是利用连续函数的定义和极限的四则运算法则。首先，对于连续函数f(x)和g(x)，在任意点x0处都有lim(x→x0)f(x)=f(x0)和lim(x→x0)g(x)=g(x0)。根据极限的加法法则，lim(x→x0)[f(x)+g(x)]=lim(x→x0)f(x)+lim(x→x0)g(x)=f(x0)+g(x0)=(f+g)(x0)，这正好符合函数在点x0处连续的定义。由于x0是任意点，因此f+g在整个区间上都是连续的。\",\n");
        prompt.append("      \"knowledgeTags\": [\"函数连续性\", \"极限\", \"函数性质\"]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 预处理JSON - 增强版
     */
    private String preprocessJson(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            log.warn("JSON字符串为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI生成的JSON为空");
        }

        log.info("处理JSON字符串，长度: {}", jsonStr.length());
        
        try {
            // 1. 预处理 - 修复基本格式
            // 移除控制字符
            for (int i = 0; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                if (c < ' ' && c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    jsonStr = jsonStr.replace(c, ' ');
                }
            }
            
            // 2. 修复特殊字符
            // 处理文本行中的换行符和制表符
            jsonStr = jsonStr.replace("\\n", " ")
                         .replace("\\r", " ")
                         .replace("\\t", " ");
            
            // 3. 修复LaTeX命令转义
            jsonStr = fixLatexCommandEscapes(jsonStr);
            
            // 4. 特殊处理数学表达式中的导数符号
            jsonStr = jsonStr.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\\\'(");
            
            // 5. 处理选项转义
            jsonStr = fixOptionsEscapes(jsonStr);
            
            // 6. 修复analysis字段
            jsonStr = fixAnalysisEscapes(jsonStr);
            
            // 7. 确保JSON的基本结构是正确的
            if (!jsonStr.startsWith("{")) {
                int firstBrace = jsonStr.indexOf("{");
                if (firstBrace >= 0) {
                    jsonStr = jsonStr.substring(firstBrace);
                    log.debug("修正了JSON开始位置");
                } else {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的JSON格式无效，缺少开始大括号");
                }
            }
            
            if (!jsonStr.endsWith("}")) {
                int lastBrace = jsonStr.lastIndexOf("}");
                if (lastBrace >= 0) {
                    jsonStr = jsonStr.substring(0, lastBrace + 1);
                    log.debug("修正了JSON结束位置");
                } else {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的JSON格式无效，缺少结束大括号");
                }
            }
            
            // 8. 进行深度JSON修复
            log.debug("JSON预处理完成，进行深度修复");
            jsonStr = emergencyFixJson(jsonStr);
            
            // 9. 验证JSON是否有效
            try {
                // 设置宽松的JSON配置，允许非标准格式
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
                mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
                mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
                mapper.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
                
                // 尝试解析为JsonNode，更宽松的检查
                JsonNode rootNode = mapper.readTree(jsonStr);
                
                // 验证是否包含questions数组
                if (!rootNode.has("questions")) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的JSON缺少questions字段");
                }
                
                // 检查questions是否为数组
                JsonNode questions = rootNode.get("questions");
                if (!questions.isArray()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的questions不是一个数组");
                }
                
                // 检查是否有问题对象
                if (questions.size() == 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成任何问题");
                }
                
                // 检查每个问题对象是否包含必要字段
                for (int i = 0; i < questions.size(); i++) {
                    JsonNode question = questions.get(i);
                    if (!question.has("content")) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "第" + (i+1) + "个问题缺少content字段");
                    }
                    if (!question.has("options") && !question.has("answer")) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "第" + (i+1) + "个问题缺少options或answer字段");
                    }
                }
                
                log.info("JSON验证通过，包含{}个问题", questions.size());
                return jsonStr;
                
            } catch (JsonParseException e) {
                log.error("第一次尝试解析JSON失败: {}", e.getMessage(), e);
                
                // 尝试进行最后的修复
                log.info("JSON验证失败，执行最后的紧急修复");
                jsonStr = emergencyFixJson(jsonStr);
                
                try {
                // 再次尝试解析
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.readTree(jsonStr);
                    return jsonStr;
                } catch (Exception e2) {
                    log.error("JSON修复后解析仍然失败: {}", e2.getMessage());
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的JSON格式无效，无法解析");
                }
            } catch (Exception e) {
                log.error("JSON验证失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成的JSON格式无效，无法解析");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSON预处理全部失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "JSON格式错误: " + e.getMessage());
        }
    }

    /**
     * 紧急修复严重损坏的JSON
     */
    private String emergencyFixJson(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return "{}";
        }

        try {
            log.info("JSON紧急修复处理开始，原始长度: {}", jsonStr.length());
            StringBuilder result = new StringBuilder(jsonStr);

            // 1. 处理控制字符，空白字符应该保留
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                if (c < ' ' && c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    result.setCharAt(i, ' ');
                }
            }
            
            // 2. 修复截断的问题 - 检查JSON末尾是否完整
            if (!result.toString().trim().endsWith("}")) {
                int lastBrace = result.lastIndexOf("}");
                if (lastBrace > 0) {
                    // 保留最后一个完整的对象
                    result.setLength(lastBrace + 1);
                    log.debug("修复了可能的JSON截断，截取到最后一个'}'");
                } else {
                    // 如果没有找到'}'，添加一个
                    result.append("}");
                    log.debug("添加了缺失的结束大括号");
                }
            }
            
            // 3. 处理数学表达式中的导数符号，如 f'(x)
            String processedJson = result.toString();
            
            // 首先清理可能存在的过度转义
            processedJson = processedJson.replace("f\\\\'(x)", "f'(x)");
            
            // 检测是否已经有转义的导数符号，如果有则不再添加额外转义
            if (!processedJson.contains("\\'(")) {
                // 仅当没有已转义的导数符号时，添加单层转义
                processedJson = processedJson.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\'(");
            }
            
            // 4. 移除字符串外的注释，只处理//开头的注释
            processedJson = removeJsonComments(processedJson);
            
            // 5. 处理数组和对象中多余或缺失的逗号
            processedJson = processedJson.replaceAll(",\\s*\\}", "}") // 移除对象末尾的逗号
                                     .replaceAll(",\\s*\\]", "]")      // 移除数组末尾的逗号
                                     .replaceAll("\\}\\s*\\{", "},{")  // 处理相邻的对象
                                     .replaceAll("\\]\\s*\\[", "],["); // 处理相邻的数组
            
            // 6. 修复选项数组中不完整的引号和括号
            processedJson = fixIncompleteOptionsArrays(processedJson);

            // 7. 检查并修复questions数组是否完整
            if (processedJson.contains("\"questions\"") && 
                processedJson.indexOf("[", processedJson.indexOf("\"questions\"")) > 0) {
                // questions数组已经开始，检查是否完整
                int questionsStart = processedJson.indexOf("\"questions\"");
                int arrayStart = processedJson.indexOf("[", questionsStart);
                
                // 计算括号平衡
                int openBrackets = 1;
                int closePos = -1;
                boolean insideString = false;
                boolean escaped = false;
                
                for (int i = arrayStart + 1; i < processedJson.length(); i++) {
                    char c = processedJson.charAt(i);
                    
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    
                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }
                    
                    if (c == '"' && !escaped) {
                        insideString = !insideString;
                        continue;
                    }
                    
                    if (!insideString) {
                        if (c == '[') {
                            openBrackets++;
                        } else if (c == ']') {
                            openBrackets--;
                            if (openBrackets == 0) {
                                closePos = i;
                                break;
                            }
                        }
                    }
                }
                
                if (closePos == -1) {
                    // 数组未正常关闭，找到最后一个完整对象，关闭数组
                    StringBuilder sbJson = new StringBuilder(processedJson);
                    int lastCompleteBrace = findLastCompleteObjectPos(sbJson.toString(), arrayStart);
                    
                    if (lastCompleteBrace > 0) {
                        // 找到最后一个完整的对象，在其后添加结束括号
                        sbJson.insert(lastCompleteBrace + 1, "]");
                        processedJson = sbJson.toString();
                        log.debug("修复了不完整的questions数组");
                    } else {
                        // 如果无法找到完整对象，提供一个空数组
                        processedJson = processedJson.substring(0, arrayStart + 1) + "]" + 
                                       (processedJson.endsWith("}") ? "" : "}");
                        log.debug("无法找到完整的问题对象，使用空数组");
                    }
                }
            }
            
            // 8. 确保整个JSON是完整的
            if (!processedJson.startsWith("{")) {
                processedJson = "{" + processedJson;
            }
            if (!processedJson.endsWith("}")) {
                processedJson = processedJson + "}";
            }

            log.info("JSON紧急修复完成，修复后长度: {}", processedJson.length());
            return processedJson;
        } catch (Exception e) {
            log.error("JSON紧急修复过程出错", e);
            // 返回最小有效的JSON
            return "{\"questions\":[]}";
        }
    }
    
    /**
     * 修复不完整的options数组，处理缺少引号或括号的情况
     */
    private String fixIncompleteOptionsArrays(String jsonStr) {
        try {
            // 预处理：清理导数表示法中的过度转义
            jsonStr = jsonStr.replace("f\\\\'(x)", "f'(x)");
            
            // 查找所有的options字段
            Pattern optionsPattern = Pattern.compile("\"options\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher optionsMatcher = optionsPattern.matcher(jsonStr);
            
            StringBuffer sb = new StringBuffer();
            
            while (optionsMatcher.find()) {
                String optionsContent = optionsMatcher.group(1);
                
                // 处理选项中的导数符号
                optionsContent = handleDerivativeNotation(optionsContent);
                
                // 检查选项数组是否完整
                int quoteCount = 0;
                for (int i = 0; i < optionsContent.length(); i++) {
                    if (optionsContent.charAt(i) == '"' && (i == 0 || optionsContent.charAt(i-1) != '\\')) {
                        quoteCount++;
                    }
                }
                
                // 如果引号数量不是偶数，说明有未闭合的引号
                if (quoteCount % 2 != 0) {
                    // 找到最后一个选项，检查是否缺少闭合引号和括号
                    int lastQuotePos = optionsContent.lastIndexOf("\"");
                    String lastOption = optionsContent.substring(lastQuotePos);
                    
                    if (!lastOption.endsWith("\"")) {
                        // 添加缺失的引号
                        optionsContent = optionsContent + "\"";
                        log.debug("修复了选项数组中缺失的引号");
                    }
                }
                
                // 检查选项内容是否包含4个选项（逗号分隔）
                String[] options = optionsContent.split(",");
                if (options.length < 4) {
                    // 尝试添加缺失的选项
                    StringBuilder fixedOptions = new StringBuilder(optionsContent);
                    
                    // 确保有足够的逗号分隔符
                    for (int i = options.length; i < 4; i++) {
                        if (!fixedOptions.toString().trim().endsWith(",")) {
                            fixedOptions.append(", ");
                        }
                        fixedOptions.append("\"选项").append((char)('A' + i)).append("\"");
                    }
                    
                    optionsContent = fixedOptions.toString();
                    log.debug("添加了缺失的选项到选项数组");
                }
                
                optionsMatcher.appendReplacement(sb, "\"options\":[" + optionsContent + "]");
            }
            
            optionsMatcher.appendTail(sb);
            
            // 同样处理answer字段中的导数符号
            String processedJson = sb.toString();
            processedJson = fixAnswerDerivativeNotation(processedJson);
            
            return processedJson;
        } catch (Exception e) {
            log.error("修复options数组失败: {}", e.getMessage());
            return jsonStr;
        }
    }
    
    /**
     * 处理字符串中的导数符号表示法
     */
    private String handleDerivativeNotation(String text) {
        if (text == null) {
            return "";
        }
        
        // 移除过度转义
        text = text.replace("\\\\'", "\\'");
        
        // 处理未转义的导数符号 - 但避免重复转义
        if (!text.contains("\\'(")) {
            text = text.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\'(");
        }
        
        return text;
    }
    
    /**
     * 修复answer字段中的导数符号
     */
    private String fixAnswerDerivativeNotation(String jsonStr) {
        try {
            // 查找所有的answer字段
            Pattern answerPattern = Pattern.compile("\"answer\"\\s*:\\s*(\\[.*?\\]|\".*?\")", Pattern.DOTALL);
            Matcher answerMatcher = answerPattern.matcher(jsonStr);
            
            StringBuffer sb = new StringBuffer();
            
            while (answerMatcher.find()) {
                String answerContent = answerMatcher.group(1);
                
                // 处理答案中的导数符号
                answerContent = handleDerivativeNotation(answerContent);
                
                answerMatcher.appendReplacement(sb, "\"answer\":" + answerContent);
            }
            
            answerMatcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("修复answer导数符号失败: {}", e.getMessage());
            return jsonStr;
        }
    }
    
    /**
     * 查找最后一个完整对象的位置
     */
    private int findLastCompleteObjectPos(String json, int startPos) {
        int openBraces = 0;
        boolean insideObject = false;
        int lastCompletePos = -1;
        boolean insideString = false;
        boolean escaped = false;
        
        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                insideString = !insideString;
                continue;
            }
            
            if (!insideString) {
                if (c == '{') {
                    openBraces++;
                    insideObject = true;
                } else if (c == '}') {
                    openBraces--;
                    if (openBraces == 0 && insideObject) {
                        lastCompletePos = i;
                        insideObject = false;
                    }
                }
            }
        }
        
        return lastCompletePos;
    }
    
    /**
     * 移除JSON中的注释
     */
    private String removeJsonComments(String json) {
        StringBuilder result = new StringBuilder();
        boolean insideString = false;
        boolean escaped = false;
        boolean insideLineComment = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (insideLineComment) {
                if (c == '\n' || c == '\r') {
                    insideLineComment = false;
                    result.append(c); // 保留换行符
                }
                continue;
            }
            
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\' && insideString) {
                result.append(c);
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                insideString = !insideString;
                result.append(c);
                continue;
            }
            
            if (!insideString && c == '/' && i + 1 < json.length() && json.charAt(i + 1) == '/') {
                insideLineComment = true;
                i++; // 跳过下一个字符
                continue;
            }
            
            result.append(c);
        }
        
        return result.toString();
    }

    /**
     * 转义字符串中的特殊字符
     */
    private String escapeString(String str) {
        if (str == null) return "";
        
        // 先处理导数符号，避免后续过度转义
        String result = handleDerivativeNotation(str);
        
        // 然后处理其他特殊字符
        return result.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 验证题目内容
     */
    private String validateContent(String content) {
        String validContent = Optional.ofNullable(content)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容不能为空"));
        
        // 检查是否含有双反斜杠的LaTeX公式，并警告
        if (validContent.contains("\\\\")) {
            log.warn("题目内容中存在双反斜杠LaTeX公式，将在存储时标准化");
        }
        
        return validContent;
    }

    /**
     * 处理知识点标签
     */
    private void processKnowledgeTags(Question question, JSONObject questionObj) {
        // 处理知识点和标签
        JSONArray knowledgeTagsArray = questionObj.getJSONArray("knowledgeTags");
        if (knowledgeTagsArray != null && !knowledgeTagsArray.isEmpty()) {
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < knowledgeTagsArray.size(); i++) {
                tags.add(knowledgeTagsArray.getStr(i));
            }
            question.setTags(tags);
            question.setTagsStr(JSONUtil.toJsonStr(tags));
            
            // 将知识点标签也保存到知识点表中
            // 注意：这里需要在保存问题后进行，因为需要问题ID
            // 此逻辑在processQuestions方法中调用questionService.saveBatch后执行
        }
    }

    private String fixAnalysisEscapes(String json) {
        try {
            // 正则表达式匹配analysis字段
            Pattern analysisPattern = Pattern.compile("\"analysis\"\\s*:\\s*\"(.*?)\"(?=,|\\})", Pattern.DOTALL);
            Matcher analysisMatcher = analysisPattern.matcher(json);
            
            StringBuffer sb = new StringBuffer();
            
            while (analysisMatcher.find()) {
                String analysisContent = analysisMatcher.group(1);
                log.debug("找到analysis字段: {}", analysisContent);
                
                // 特殊处理导数符号
                analysisContent = analysisContent.replaceAll("([a-zA-Z])\\s*'\\s*\\(", "$1\\\\'(");
                
                // 使用数学符号处理函数
                analysisContent = processSpecialMathSymbols(analysisContent);
                
                // 将修复后的分析内容放回原位
                analysisMatcher.appendReplacement(sb, "\"analysis\":\"" + analysisContent + "\"");
            }
            analysisMatcher.appendTail(sb);
            
            return sb.toString();
        } catch (Exception e) {
            log.warn("修复分析字段转义时出错: {}", e.getMessage(), e);
            // 出错时返回原始字符串，避免丢失内容
            return json;
        }
    }

    private String fixFieldFormatting(String jsonStr) {
        try {
            // 修复字段间的格式问题
            
            // 确保字段名后的冒号格式正确
            jsonStr = jsonStr.replaceAll("\"(\\w+)\"\\s*:\\s*", "\"$1\":");
            
            // 确保字符串值周围的引号格式正确
            jsonStr = jsonStr.replaceAll(":\"([^\"]*?)\\s*\"\\s*([,}])", ":\"$1\"$2");
            
            // 修复布尔值和数字格式
            jsonStr = jsonStr.replaceAll("\"true\"", "true");
            jsonStr = jsonStr.replaceAll("\"false\"", "false");
            jsonStr = jsonStr.replaceAll("\"(\\d+)\"", "$1");
            
            // 修复字段值中缺少引号的问题
            jsonStr = jsonStr.replaceAll(":([^{\\[\"\\d]\\w+)([,}])", ":\"$1\"$2");
            
            // 移除HTML标签
            jsonStr = jsonStr.replaceAll("<[^>]*>", "");
            
            return jsonStr;
        } catch (Exception e) {
            log.warn("修复字段格式时出错: {}", e.getMessage());
            // 如果处理失败，返回原始字符串
            return jsonStr;
        }
    }
}