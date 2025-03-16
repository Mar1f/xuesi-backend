package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.ScoringStrategyEnum;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.QuestionBankMapper;
import com.xuesi.xuesisi.mapper.ScoringResultMapper;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.service.ScoringResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONConfig;
import org.apache.commons.lang3.StringUtils;
import java.util.Optional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private DeepSeekService deepseekService;

    @Resource
    private ScoringResultMapper scoringResultMapper;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestionBank(QuestionBank questionBank) {
        // 参数校验
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验题目数量
        if (questionBank.getQuestionCount() == null || questionBank.getQuestionCount() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目数量必须大于0");
        }
        // 设置默认值
        questionBank.setCreateTime(new Date());
        questionBank.setUpdateTime(new Date());
        questionBank.setIsDelete(0);
        questionBank.setReviewStatus(ReviewStatusEnum.WAITING.getValue());
        // 保存
        boolean result = questionBankMapper.insert(questionBank) > 0;
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 如果是AI评分策略，自动生成题目
        if (questionBank.getScoringStrategy() == ScoringStrategyEnum.AI.getValue()) {
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
                prompt.append("   - knowledgeTags: 知识点标签数组\n");
                prompt.append("4. 所有字符串必须使用英文双引号\n");
                prompt.append("5. 不要包含任何注释或额外说明\n");
                prompt.append("6. 不要使用markdown代码块\n");
                prompt.append("7. 不要包含任何其他文本\n");
                prompt.append("8. 对于数学公式，请使用以下格式：\n");
                prompt.append("   - 行内公式：使用 \\\\\\( 和 \\\\\\\\) 包裹\n");
                prompt.append("   - 行间公式：使用 \\\\\\\\[ 和 \\\\\\\\] 包裹\n\n");

                prompt.append("示例格式：\n");
                prompt.append("{\n");
                prompt.append("  \"questions\": [\n");
                prompt.append("    {\n");
                prompt.append("      \"content\": \"解方程 \\\\\\\\( x^2 - 5x + 6 = 0 \\\\\\\\)\",\n");
                prompt.append("      \"options\": [\"x=2或x=3\", \"x=1或x=6\", \"x=0或x=5\", \"x=4或x=7\"],\n");
                prompt.append("      \"answer\": \"A\",\n");
                prompt.append("      \"analysis\": \"通过因式分解法，方程可分解为(x-2)(x-3)=0，因此解为x=2或x=3。\",\n");
                prompt.append("      \"knowledgeTags\": [\"二次方程\", \"因式分解\"]\n");
                prompt.append("    }\n");
                prompt.append("  ]\n");
                prompt.append("}\n\n");

                prompt.append("请根据以下主题生成题目：\n");
                prompt.append(questionBank.getDescription());

                // 调用DeepSeek生成题目
                String aiResponse = deepseekService.chat(prompt.toString());
                log.info("AI响应内容: {}", aiResponse);

                // 预处理JSON字符串
                String jsonStr = preprocessJson(aiResponse);
                log.debug("提取的JSON内容: {}", jsonStr);
                
                jsonStr = preprocessJson(jsonStr);
                log.debug("预处理后的JSON: {}", jsonStr);
                
                // 验证JSON结构
                validateJsonStructure(jsonStr);
                
                // 解析JSON
                JSONObject jsonResponse = JSONUtil.parseObj(jsonStr, JSONConfig.create()
                    .setIgnoreCase(true)
                    .setCheckDuplicate(false));

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

                // 处理题目
                List<Question> questionsList = processQuestions(jsonResponse, questionBank.getId(), questionBank);
                if (questionsList.isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未生成有效的题目");
                }

            } catch (Exception e) {
                log.error("AI生成题目失败，将使用默认题目", e);
                createDefaultQuestionsFallback(questionBank.getId(), questionBank.getUserId());
            }
        }

        // 创建评分结果
        scoringResultService.createDefaultScoringResults(questionBank);

        return questionBank.getId();
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
    private String preprocessJson(String aiResponse) {
        if (StringUtils.isBlank(aiResponse)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI响应为空");
        }
        
        log.info("AI响应内容: {}", aiResponse);
        
        try {
            // 1. 移除markdown代码块标记
            aiResponse = aiResponse.replaceAll("```json\\s*", "")
                                 .replaceAll("\\s*```", "")
                                 .trim();
                                 
            // 2. 替换LaTeX表达式
            // 先处理行内LaTeX
            aiResponse = aiResponse.replaceAll("\\\\\\((.*?)\\\\\\)", "($1)")
                                 .replaceAll("\\\\\\[(.*?)\\\\\\]", "[$1]");
                                 
            // 3. 替换特殊的LaTeX命令
            aiResponse = aiResponse.replaceAll("\\\\begin\\{cases\\}", "[")
                                 .replaceAll("\\\\end\\{cases\\}", "]")
                                 .replaceAll("\\\\\\\\", "\\\\")  // 处理换行
                                 .replaceAll("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}", "frac{$1}{$2}")
                                 .replaceAll("\\\\sqrt\\{([^}]*)\\}", "sqrt{$1}")
                                 .replaceAll("\\\\times", "times")
                                 .replaceAll("\\\\left", "")
                                 .replaceAll("\\\\right", "");
                                 
            // 4. 处理选项数组格式
            Pattern arrayPattern = Pattern.compile("\"options\":\\s*\\[(.*?)\\]");
            Matcher arrayMatcher = arrayPattern.matcher(aiResponse);
            StringBuffer sb = new StringBuffer();
            while (arrayMatcher.find()) {
                String options = arrayMatcher.group(1);
                // 如果选项本身是数组，保持原样
                if (options.contains("[") && options.contains("]")) {
                    continue;
                }
                
                // 处理坐标对选项的特殊情况
                if (options.contains(",") && options.contains("(") && options.contains(")")) {
                    String[] rawOptions = options.split(",(?=\\s*[\"\\(])");
                    List<String> formattedOptions = new ArrayList<>();
                    for (String option : rawOptions) {
                        option = option.trim();
                        if (!option.startsWith("\"")) {
                            option = "\"" + option;
                        }
                        if (!option.endsWith("\"")) {
                            option = option + "\"";
                        }
                        formattedOptions.add(option);
                    }
                    String replacement = "\"options\": [" + String.join(",", formattedOptions) + "]";
                    arrayMatcher.appendReplacement(sb, replacement);
                    continue;
                }
                
                // 处理普通选项
                String[] optionArray = options.split(",");
                List<String> formattedOptions = new ArrayList<>();
                for (String option : optionArray) {
                    option = option.trim();
                    if (!option.startsWith("\"")) {
                        option = "\"" + option;
                    }
                    if (!option.endsWith("\"")) {
                        option = option + "\"";
                    }
                    formattedOptions.add(option);
                }
                String replacement = "\"options\": [" + String.join(",", formattedOptions) + "]";
                arrayMatcher.appendReplacement(sb, replacement);
            }
            arrayMatcher.appendTail(sb);
            aiResponse = sb.toString();
            
            log.info("预处理后的JSON字符串: {}", aiResponse);
            
            // 5. 验证JSON结构
            JSONObject jsonObject = JSONUtil.parseObj(aiResponse);
            return aiResponse;
            
        } catch (Exception e) {
            log.error("JSON结构验证失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "JSON格式错误: " + e.getMessage());
        }
    }

    /**
     * 验证JSON结构
     */
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
                String[] requiredFields = {"content", "options", "answer", "analysis", "knowledgeTags"};
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
     * 处理题目数据
     */
    private List<Question> processQuestions(JSONObject jsonResponse, Long bankId, QuestionBank questionBank) {
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
                question.setUserId(questionBank.getUserId());
                
                // 处理标签
                JSONArray tags = questionObj.getJSONArray("knowledgeTags");
                if (tags != null && !tags.isEmpty()) {
                    List<String> tagList = new ArrayList<>();
                    for (int j = 0; j < tags.size(); j++) {
                        String tag = tags.getStr(j);
                        if (StringUtils.isNotBlank(tag)) {
                            tagList.add(tag);
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
                questionBankQuestion.setUserId(questionBank.getUserId());
                
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
     * 验证题目内容
     */
    private String validateContent(String content) {
        return Optional.ofNullable(content)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容不能为空"));
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
        // 删除题库
        return removeById(id);
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
        // TODO: 实现获取题目列表的逻辑
        return null;
    }

    @Override
    public ScoringResultVO initializeScoringResult(Long questionBankId, Long userId) {
        // 参数校验
        if (questionBankId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 初始化评分结果
        // TODO: 实现初始化评分结果的逻辑
        return null;
    }

    @Override
    public ScoringResultVO submitAnswers(Long questionBankId, Long userId, List<String> answers) {
        // 参数校验
        if (questionBankId == null || userId == null || answers == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查题库是否存在
        QuestionBank questionBank = getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 提交答案并评分
        // TODO: 实现提交答案并评分的逻辑
        return null;
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
        // 获取评分历史
        // TODO: 实现获取评分历史的逻辑
        return null;
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
        // 获取题库统计信息
        // TODO: 实现获取统计信息的逻辑
        return convertToVO(questionBank);
    }

    /**
     * 将实体转换为VO
     */
    private QuestionBankVO convertToVO(QuestionBank questionBank) {
        if (questionBank == null) {
            return null;
        }
        QuestionBankVO vo = new QuestionBankVO();
        BeanUtils.copyProperties(questionBank, vo);
        return vo;
    }
}