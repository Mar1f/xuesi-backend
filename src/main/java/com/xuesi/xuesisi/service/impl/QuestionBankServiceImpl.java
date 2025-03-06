package com.xuesi.xuesisi.service.impl;

import cn.hutool.json.JSONConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.QuestionBankMapper;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.service.UserService;
import com.xuesi.xuesisi.service.KnowledgePointService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;

/**
* @author mar1
* @description 针对表【question_bank(题单表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
@Slf4j
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private KnowledgePointService knowledgePointService;

    /**
     * 校验数据
     *
     * @param questionBank
     * @param add          对创建的数据进行校验
     */
    @Override
    public void validQuestionBank(QuestionBank questionBank, boolean add) {
        if (questionBank == null) {
            throw new IllegalArgumentException("题单不能为空");
        }
        if (add) {
            if (questionBank.getTitle() == null || questionBank.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("题单标题不能为空");
            }
            if (questionBank.getDescription() == null || questionBank.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("题单描述不能为空");
            }
            if (questionBank.getSubject() == null || questionBank.getSubject().trim().isEmpty()) {
                throw new IllegalArgumentException("学科不能为空");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBank createQuestionBank(QuestionBank questionBank) {
        try {
            // 参数校验
            validQuestionBank(questionBank, true);
            
            // 验证题目数量
            if (questionBank.getQuestionCount() == null || questionBank.getQuestionCount() < 1 || questionBank.getQuestionCount() > 50) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目数量必须在1-50之间");
            }

            // 获取当前登录用户
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            User loginUser = userService.getLoginUser(request);
            questionBank.setUserId(loginUser.getId());

            // 插入数据
            boolean save = save(questionBank);
            ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
            long newQuestionBankId = questionBank.getId();

            // 创建默认评分结果
            scoringResultService.createDefaultScoringResults(questionBank);

            // 使用AI生成题目
            try {
                // 构建AI提示词
                StringBuilder prompt = new StringBuilder();
                prompt.append("你是一个专业的题目生成器。请生成").append(questionBank.getQuestionCount())
                      .append("个").append(questionBank.getSubject()).append("学科的题目，除专业术语外用中文，必须严格按照以下JSON格式返回。注意：\n\n");
                prompt.append("1. 必须返回合法的JSON格式\n");
                prompt.append("2. JSON必须包含questions数组\n");
                prompt.append("3. 每个题目必须包含以下字段：\n");
                prompt.append("   - content: 题目内容\n");
                prompt.append("   - options: 包含4个选项的数组\n");
                prompt.append("   - answer: 正确答案（A、B、C、D中的一个）\n");
                prompt.append("   - analysis: 答案解析\n");
                prompt.append("   - tags: 知识点标签数组，每个题目至少包含1个标签\n");
                prompt.append("4. 所有字符串必须使用英文双引号\n");
                prompt.append("5. 不要包含任何注释或额外说明\n");
                prompt.append("6. 不要使用markdown代码块\n");
                prompt.append("7. 不要包含任何其他文本\n");
                prompt.append("8. 标签必须与题目内容相关，且符合").append(questionBank.getSubject()).append("学科知识体系\n\n");

                prompt.append("示例格式：\n");
                prompt.append("{\n");
                prompt.append("  \"questions\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"content\": \"1 + 1 = ?\",\n");
                prompt.append("      \"options\": [\"A. 2\", \"B. 3\", \"C. 4\", \"D. 5\"],\n");
                prompt.append("      \"answer\": \"A\",\n");
                prompt.append("      \"analysis\": \"1加1等于2，所以答案是A\",\n");
                prompt.append("      \"tags\": [\"基础运算\", \"加法\"]\n");
                prompt.append("    }\n");
                prompt.append("  ]\n");
                prompt.append("}\n\n");

                prompt.append("请根据以下主题生成题目：\n");
                prompt.append(questionBank.getDescription());

                // 调用DeepSeek生成题目
                String aiResponse = deepSeekService.chat(prompt.toString());
                log.info("AI响应内容: {}", aiResponse);

                // 预处理JSON字符串
                String jsonStr = extractJsonContent(aiResponse);
                log.debug("提取的JSON内容: {}", jsonStr);
                
                jsonStr = preprocessJson(jsonStr);
                log.debug("预处理后的JSON: {}", jsonStr);
                
                // 验证JSON结构
                validateJsonStructure(jsonStr);
                
                // 解析JSON
                JSONObject jsonResponse = JSONUtil.parseObj(jsonStr, JSONConfig.create()
                    .setOrder(false)
                    .setCheckDuplicate(false)
                    .setIgnoreCase(true));

                // 验证必要字段
                if (!jsonResponse.containsKey("questions")) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应缺少questions字段");
                }

                // 验证题目数量
                JSONArray questions = jsonResponse.getJSONArray("questions");
                if (questions == null || questions.isEmpty() || questions.size() < questionBank.getQuestionCount()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("AI未生成足够的题目，期望: %d, 实际: %d", 
                            questionBank.getQuestionCount(), 
                            questions != null ? questions.size() : 0));
                }

                // 验证每个题目的格式
                for (int i = 0; i < questions.size(); i++) {
                    JSONObject question = questions.getJSONObject(i);
                    if (!question.containsKey("content") || !question.containsKey("options") 
                        || !question.containsKey("answer") || !question.containsKey("analysis")) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                            String.format("第%d题缺少必要字段", i + 1));
                    }
                    
                    JSONArray options = question.getJSONArray("options");
                    if (options == null || options.size() != 4) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                            String.format("第%d题选项数量不正确，期望: 4, 实际: %d", 
                                i + 1, options != null ? options.size() : 0));
                    }
                    
                    String answer = question.getStr("answer");
                    if (!answer.matches("[A-Da-d]")) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                            String.format("第%d题答案格式不正确: %s", i + 1, answer));
                    }
                }

                // 处理题目
                List<Question> questionsList = processQuestions(jsonResponse, newQuestionBankId, loginUser, questionBank);
                if (questionsList.isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成有效的题目");
                }

                log.info("成功创建题单，ID: {}, 题目数量: {}", newQuestionBankId, questionsList.size());
                return questionBank;

            } catch (Exception e) {
                log.error("AI生成题目失败，将使用默认题目", e);
                createDefaultQuestionsFallback(newQuestionBankId, loginUser.getId());
                return questionBank;
            }
        } catch (BusinessException e) {
            log.error("创建题单失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建题单时发生未知错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建题单失败: " + e.getMessage());
        }
    }

    /**
     * 从响应中提取有效JSON内容
     */
    private String extractJsonContent(String aiResponse) {
        // 移除可能的markdown代码块标记
        String cleanResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
        
        // 查找第一个 { 和最后一个 }
        int startIndex = cleanResponse.indexOf("{");
        int endIndex = cleanResponse.lastIndexOf("}");
        
        if (startIndex == -1 || endIndex == -1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应中未找到有效的JSON内容");
        }
        
        return cleanResponse.substring(startIndex, endIndex + 1);
    }

    /**
     * JSON预处理
     */
    private String preprocessJson(String jsonStr) {
        // 移除 Markdown 代码块标记
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }
        }
        jsonStr = jsonStr.trim();
        
        // 预处理 JSON 字符串
        jsonStr = jsonStr.replaceAll("\\\\n\\s*", "") // 移除转义的换行符及其后的空白
                       .replaceAll("\\s*\\\\\"\\s*", "\"") // 处理转义的引号
                       .replaceAll("\\\\([^\"\\\\])", "$1") // 移除不必要的反斜杠
                       .replaceAll("\"\\s*:\\s*\"", "\": \"") // 规范化键值对格式
                       .replaceAll(",\\s*}", "}") // 处理对象末尾多余的逗号
                       .replaceAll(",\\s*]", "]"); // 处理数组末尾多余的逗号
        
        // 处理特殊字符和转义序列
        jsonStr = jsonStr.replace("\\\"", "\"")
                       .replace("\\n", "")
                       .replace("\\r", "")
                       .replace("\\t", "");
        
        // 处理 LaTeX 公式中的反斜杠
        jsonStr = jsonStr.replace("\\(", "\\\\(")
                       .replace("\\)", "\\\\)")
                       .replace("\\frac", "\\\\frac")
                       .replace("\\sqrt", "\\\\sqrt")
                       .replace("\\neq", "\\\\neq");
        
        log.info("预处理后的JSON字符串: {}", jsonStr);
        
        return jsonStr;
    }

    // 修改 validateJsonStructure 方法
    private void validateJsonStructure(String jsonStr) {
        try {
            JSONObject jsonResponse = JSONUtil.parseObj(jsonStr);
            
            // 验证questions字段
            if (!jsonResponse.containsKey("questions")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "JSON缺少questions字段");
            }
            
            JSONArray questions = jsonResponse.getJSONArray("questions");
            if (questions == null || questions.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "questions数组为空");
            }
            
            // 验证每个题目的格式
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                
                // 验证必要字段
                String[] requiredFields = {"content", "options", "answer", "analysis"};
                for (String field : requiredFields) {
                    if (!question.containsKey(field)) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                            String.format("第%d题缺少%s字段", i + 1, field));
                    }
                }
                
                // 验证选项格式
                JSONArray options = question.getJSONArray("options");
                if (options == null || options.size() != 4) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("第%d题选项数量不正确，应为4个", i + 1));
                }
                
                // 验证答案格式
                String answer = question.getStr("answer");
                if (!answer.matches("[A-Da-d]")) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("第%d题答案格式不正确，应为A-D", i + 1));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSON结构验证失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "JSON结构验证失败");
        }
    }
    /**
     * 验证题目内容
     */
    private String validateContent(String content) {
        return Optional.ofNullable(content)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容不能为空"));
    }

    /**
     * 处理题目数据
     */
    private List<Question> processQuestions(JSONObject jsonResponse, Long bankId, User user, QuestionBank questionBank) {
        List<Question> questions = new ArrayList<>();
        try {
            JSONArray questionsArray = jsonResponse.getJSONArray("questions");
            int questionCount = questionsArray.size();
            
            for (int i = 0; i < questionCount; i++) {
                JSONObject questionObj = questionsArray.getJSONObject(i);
                Question question = new Question();
                question.setQuestionContent(validateContent(questionObj.getStr("content")));
                question.setQuestionType(0); // 默认为单选题
                question.setOptions(processOptions(questionObj.getJSONArray("options")));
                question.setAnswer(processAnswer(questionObj.getStr("answer")));
                question.setAnalysis(questionObj.getStr("analysis"));
                question.setScore(100 / questionCount); // 根据题目数量计算每道题的分值
                question.setSource(1); // AI生成
                question.setUserId(user.getId());
                
                // 处理标签
                JSONArray tags = questionObj.getJSONArray("tags");
                if (tags != null && !tags.isEmpty()) {
                    List<String> tagList = new ArrayList<>();
                    for (int j = 0; j < tags.size(); j++) {
                        String tag = tags.getStr(j);
                        if (StringUtils.isNotBlank(tag)) {
                            tagList.add(tag);
                            // 创建或更新知识点
                            KnowledgePoint knowledgePoint = new KnowledgePoint();
                            knowledgePoint.setName(tag);
                            knowledgePoint.setSubject(questionBank.getSubject());
                            knowledgePoint.setDescription("知识点：" + tag);
                            knowledgePoint.setUserId(user.getId());
                            knowledgePointService.saveOrUpdate(knowledgePoint);
                        }
                    }
                    question.setTags(tagList);
                }
                
                // 保存题目
                boolean questionSaved = questionService.save(question);
                if (!questionSaved) {
                    log.error("保存AI生成题目失败");
                    continue;
                }
                
                // 创建题目和题单的关联
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(bankId);
                questionBankQuestion.setQuestionId(question.getId());
                questionBankQuestion.setQuestionOrder(i + 1);
                questionBankQuestion.setUserId(user.getId());
                
                questionBankQuestionService.save(questionBankQuestion);
                
                questions.add(question);
            }
            
            // 验证生成的题目数量
            if (questions.size() < questionCount) {
                log.error("AI生成的题目数量不足，期望: {}, 实际: {}", questionCount, questions.size());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成足够的题目");
            }
            
        } catch (Exception e) {
            log.error("处理AI生成的题目失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理AI生成的题目失败: " + e.getMessage());
        }
        
        return questions;
    }

    /**
     * 处理答案格式
     */
    private List<String> processAnswer(String answer) {
        String cleaned = Optional.ofNullable(answer)
                .map(String::trim)
                .map(s -> s.replaceAll("[^A-Da-d]", ""))
                .map(String::toUpperCase)
                .filter(s -> s.length() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "答案格式无效"));

        return Collections.singletonList(cleaned);
    }

    /**
     * 处理选项格式
     */
    private List<String> processOptions(JSONArray options) {
        if (options == null || options.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "选项不能为空");
        }

        List<String> optionList = new ArrayList<>();
        for (Object option : options) {
            String optionStr = Optional.ofNullable(option)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "选项不能为空"));
            optionList.add(optionStr);
        }

        if (optionList.size() != 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须包含4个有效选项");
        }
        return optionList;
    }

    /**
     * 创建默认题目（备用方案）
     */
    private void createDefaultQuestionsFallback(Long questionBankId, Long userId) {
        // 默认题目
        String[] defaultQuestions = {
            "1 + 1 = ?",
            "2 + 2 = ?",
            "3 + 3 = ?",
            "4 + 4 = ?",
            "5 + 5 = ?"
        };
        
        // 默认选项
        String[] defaultOptions = {
            "A. 2, B. 3, C. 4, D. 5",
            "A. 3, B. 4, C. 5, D. 6",
            "A. 4, B. 5, C. 6, D. 7",
            "A. 5, B. 6, C. 7, D. 8",
            "A. 6, B. 7, C. 8, D. 9"
        };
        
        // 默认答案
        String[] defaultAnswers = {
            "A",
            "B",
            "C",
            "D",
            "A"
        };
        
        int questionCount = defaultQuestions.length;
        
        for (int i = 0; i < questionCount; i++) {
            // 创建题目实体
            Question question = new Question();
            question.setQuestionContent(defaultQuestions[i]);
            question.setQuestionType(0); // 默认为单选题
            question.setOptions(Arrays.asList(defaultOptions[i]));
            question.setAnswer(Collections.singletonList(defaultAnswers[i]));
            question.setScore(100 / questionCount); // 根据题目数量计算每道题的分值
            question.setSource(0); // 标记为手动创建
            question.setUserId(userId);
            
            // 保存题目
            boolean questionSaved = questionService.save(question);
            if (!questionSaved) {
                log.error("保存默认题目失败");
                continue;
            }
            
            // 创建题目和题单的关联
            QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
            questionBankQuestion.setQuestionBankId(questionBankId);
            questionBankQuestion.setQuestionId(question.getId());
            questionBankQuestion.setQuestionOrder(i + 1);
            questionBankQuestion.setUserId(userId);
            
            questionBankQuestionService.save(questionBankQuestion);
        }
    }

    @Override
    public QuestionBank getQuestionBankById(Long id) {
        return getById(id);
    }

    @Override
    public List<QuestionBank> getAllQuestionBanks() {
        return list();
    }

    @Override
    public QuestionBank updateQuestionBank(Long id, QuestionBank questionBank) {
        questionBank.setId(id);
        if (updateById(questionBank)) {
            return getById(id);
        }
        return null;
    }

    @Override
    public void deleteQuestionBank(Long id) {
        removeById(id);
    }

    @Override
    public Page<QuestionBank> getQuestionBanksPage(int pageNumber, int pageSize) {
        Page<QuestionBank> page = new Page<>(pageNumber, pageSize);
        return page(page);
    }

    @Override
    public void initScoringResults(QuestionBank questionBank) {
        scoringResultService.createDefaultScoringResults(questionBank);
    }

    @Override
    public long addQuestionBank(QuestionBank questionBank) {
        // 参数校验
        validQuestionBank(questionBank, true);
        
        // 获取当前登录用户
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        
        // 插入数据
        boolean save = save(questionBank);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        long newQuestionBankId = questionBank.getId();

        // 创建默认评分结果
        scoringResultService.createDefaultScoringResults(questionBank);

        // 使用AI生成题目
        try {
            // 构建AI提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一个专业的题目生成器。请生成10个题目，必须严格按照以下JSON格式返回，不要包含任何其他内容。\n\n");
            prompt.append("格式要求：\n");
            prompt.append("1. 必须是一个包含questions数组的JSON对象\n");
            prompt.append("2. 每个题目必须包含content、options、answer和analysis四个字段\n");
            prompt.append("3. 所有字符串必须使用英文双引号\n");
            prompt.append("4. 不允许有任何注释、思考过程或额外说明\n");
            prompt.append("5. 数组最后一项后不能有逗号\n");
            prompt.append("6. 必须是合法的JSON格式\n");
            prompt.append("7. 每个题目必须包含4个选项\n");
            prompt.append("8. 答案必须是A、B、C、D中的一个\n");
            prompt.append("9. 不允许使用中文引号或其他特殊字符\n");
            prompt.append("10. 不允许在JSON中包含任何换行符\n\n");

            prompt.append("题目要求：\n");
            prompt.append("1. 题目难度要适中\n");
            prompt.append("2. 选项要清晰明确\n");
            prompt.append("3. 解析要详细易懂\n\n");

            prompt.append("示例格式：\n");
            prompt.append("{\"questions\":[{\"content\":\"What is the capital of France?\",\"options\":[\"London\",\"Paris\",\"Berlin\",\"Madrid\"],\"answer\":\"B\",\"analysis\":\"Paris is the capital city of France.\"}]}\n\n");

            prompt.append("请根据以下主题生成题目（记住：只返回JSON，不要有任何其他内容）：\n");
            prompt.append(questionBank.getDescription());

            // 调用DeepSeek生成题目
            String aiResponse = deepSeekService.chat(prompt.toString());
            log.info("AI响应内容: {}", aiResponse);

            // 预处理JSON字符串
            String jsonStr = extractJsonContent(aiResponse);
            jsonStr = preprocessJson(jsonStr);
            
            // 验证JSON结构
            validateJsonStructure(jsonStr);
            
            // 解析JSON
            JSONObject jsonResponse = JSONUtil.parseObj(jsonStr, JSONConfig.create()
                .setOrder(false)
                .setCheckDuplicate(false)
                .setIgnoreCase(true));

            // 验证必要字段
            if (!jsonResponse.containsKey("questions")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应缺少questions字段");
            }

            JSONArray questions = jsonResponse.getJSONArray("questions");
            if (questions == null || questions.isEmpty() || questions.size() < 10) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成足够的题目");
            }

            // 验证每个题目的格式
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                if (!question.containsKey("content") || !question.containsKey("options") 
                    || !question.containsKey("answer") || !question.containsKey("analysis")) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("第%d题缺少必要字段", i + 1));
                }
                
                JSONArray options = question.getJSONArray("options");
                if (options == null || options.size() != 4) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("第%d题选项数量不正确", i + 1));
                }
                
                String answer = question.getStr("answer");
                if (!answer.matches("[A-Da-d]")) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                        String.format("第%d题答案格式不正确", i + 1));
                }
            }

            // 处理题目
            processQuestions(jsonResponse, newQuestionBankId, loginUser, questionBank);
        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            // 如果AI生成失败，使用默认题目作为备用
            createDefaultQuestionsFallback(newQuestionBankId, loginUser.getId());
        }

        return newQuestionBankId;
    }

    private List<Question> processQuestions(String aiResponse, int questionCount) {
        List<Question> questions = new ArrayList<>();
        try {
            // 预处理JSON字符串，移除可能的特殊字符和格式问题
            String cleanJson = aiResponse.replaceAll("[\u0000-\u001F\u007F-\u009F]", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            
            // 尝试提取JSON对象
            int startIndex = cleanJson.indexOf("{");
            int endIndex = cleanJson.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                log.error("无法在AI响应中找到有效的JSON对象");
                return questions;
            }
            
            String jsonStr = cleanJson.substring(startIndex, endIndex + 1);
            log.info("预处理后的JSON字符串: {}", jsonStr);
            
            // 使用ObjectMapper解析JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonStr);
            JsonNode questionsNode = rootNode.get("questions");
            
            if (questionsNode == null || !questionsNode.isArray()) {
                log.error("AI响应中没有找到questions数组");
                return questions;
            }
            
            // 处理每个题目
            for (JsonNode questionNode : questionsNode) {
                try {
                    Question question = new Question();
                    question.setQuestionContent(questionNode.get("content").asText());
                    question.setQuestionType(0); // 单选题
                    
                    // 处理选项
                    JsonNode optionsNode = questionNode.get("options");
                    if (optionsNode != null && optionsNode.isArray()) {
                        List<String> options = new ArrayList<>();
                        for (JsonNode option : optionsNode) {
                            options.add(option.asText());
                        }
                        question.setOptions(options);
                    }
                    
                    // 处理答案
                    JsonNode answerNode = questionNode.get("answer");
                    if (answerNode != null) {
                        List<String> answers = new ArrayList<>();
                        answers.add(answerNode.asText());
                        question.setAnswer(answers);
                    }
                    
                    // 处理解析
                    JsonNode analysisNode = questionNode.get("analysis");
                    if (analysisNode != null) {
                        question.setAnalysis(analysisNode.asText());
                    }
                    
                    // 设置分数
                    question.setScore(100 / questionCount);
                    question.setSource(0); // AI生成
                    question.setUserId(1L); // 默认用户ID
                    
                    questions.add(question);
                } catch (Exception e) {
                    log.warn("解析题目对象失败，跳过: {}", e.getMessage());
                    continue;
                }
            }
            
            // 验证生成的题目数量
            if (questions.size() < questionCount) {
                log.error("AI生成的题目数量不足，期望: {}, 实际: {}", questionCount, questions.size());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成足够的题目");
            }
            
        } catch (Exception e) {
            log.error("处理AI生成的题目失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理AI生成的题目失败: " + e.getMessage());
        }
        
        return questions;
    }
}




