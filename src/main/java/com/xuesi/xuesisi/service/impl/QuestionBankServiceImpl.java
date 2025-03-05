package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    /**
     * 校验数据
     *
     * @param questionBank
     * @param add           对创建的数据进行校验
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
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBank createQuestionBank(QuestionBank questionBank) {
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
            prompt.append("请根据以下题单内容，生成10个相关的题目。每个题目需要包含：\n");
            prompt.append("1. 题目内容\n");
            prompt.append("2. 选项（A、B、C、D）\n");
            prompt.append("3. 正确答案\n");
            prompt.append("4. 题目解析\n\n");
            prompt.append("题单内容：\n");
            prompt.append(questionBank.getDescription());
            prompt.append("\n\n请以JSON格式返回，格式如下：\n");
            prompt.append("{\n");
            prompt.append("  \"questions\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"content\": \"题目内容\",\n");
            prompt.append("      \"options\": [\"A选项\", \"B选项\", \"C选项\", \"D选项\"],\n");
            prompt.append("      \"answer\": \"正确答案\",\n");
            prompt.append("      \"analysis\": \"题目解析\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}");

            // 调用DeepSeek生成题目
            String aiResponse = deepSeekService.chat(prompt.toString());
            
            // 解析AI响应
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            JSONArray questions = jsonResponse.getJSONArray("questions");
            
            // 保存题目
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                
                // 创建题目实体
                Question questionEntity = new Question();
                questionEntity.setQuestionContent(question.getStr("content"));
                questionEntity.setQuestionType(0); // 默认为单选题
                questionEntity.setOptions(question.getJSONArray("options").toList(String.class));
                questionEntity.setAnswer(Collections.singletonList(question.getStr("answer")));
                questionEntity.setScore(10); // 默认每题10分
                questionEntity.setSource(1); // 标记为AI生成
                questionEntity.setUserId(loginUser.getId());
                
                // 保存题目
                boolean questionSaved = questionService.save(questionEntity);
                if (!questionSaved) {
                    log.error("保存题目失败");
                    continue;
                }
                
                // 创建题目和题单的关联
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(newQuestionBankId);
                questionBankQuestion.setQuestionId(questionEntity.getId());
                questionBankQuestion.setQuestionOrder(i + 1);
                questionBankQuestion.setUserId(loginUser.getId());
                
                questionBankQuestionService.save(questionBankQuestion);
            }
        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            // 如果AI生成失败，使用默认题目作为备用
            createDefaultQuestionsFallback(newQuestionBankId, loginUser.getId());
        }

        return questionBank;
    }

    /**
     * 创建默认题目（备用方案）
     */
    private void createDefaultQuestionsFallback(Long questionBankId, Long userId) {
        // 创建5个默认题目
        String[] defaultQuestions = {
            "Java中的基本数据类型不包括以下哪个？",
            "以下哪个不是Java集合框架中的接口？",
            "Java中用于声明常量的关键字是？",
            "Java中的方法重载是指？",
            "Java中的final关键字可以修饰哪些内容？"
        };

        String[][] defaultOptions = {
            {"int", "String", "double", "boolean"},
            {"List", "Set", "Map", "String"},
            {"final", "const", "static", "private"},
            {"方法名相同，参数不同", "方法名不同，参数相同", "方法名和参数都不同", "以上都不对"},
            {"类、方法、变量", "只有方法", "只有变量", "只有类"}
        };

        String[] defaultAnswers = {
            "B",  // String不是基本数据类型
            "D",  // String不是集合框架接口
            "A",  // final是声明常量的关键字
            "A",  // 方法重载是方法名相同，参数不同
            "A"   // final可以修饰类、方法、变量
        };

        for (int i = 0; i < defaultQuestions.length; i++) {
            // 创建题目实体
            Question question = new Question();
            question.setQuestionContent(defaultQuestions[i]);
            question.setQuestionType(0); // 默认为单选题
            question.setOptions(Arrays.asList(defaultOptions[i]));
            question.setAnswer(Collections.singletonList(defaultAnswers[i]));
            question.setScore(10); // 默认每题10分
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
            prompt.append("请根据以下题单内容，生成10个相关的题目。每个题目需要包含：\n");
            prompt.append("1. 题目内容\n");
            prompt.append("2. 选项（A、B、C、D）\n");
            prompt.append("3. 正确答案\n");
            prompt.append("4. 题目解析\n\n");
            prompt.append("题单内容：\n");
            prompt.append(questionBank.getDescription());
            prompt.append("\n\n请以JSON格式返回，格式如下：\n");
            prompt.append("{\n");
            prompt.append("  \"questions\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"content\": \"题目内容\",\n");
            prompt.append("      \"options\": [\"A选项\", \"B选项\", \"C选项\", \"D选项\"],\n");
            prompt.append("      \"answer\": \"正确答案\",\n");
            prompt.append("      \"analysis\": \"题目解析\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}");

            // 调用DeepSeek生成题目
            String aiResponse = deepSeekService.chat(prompt.toString());
            
            // 解析AI响应
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            JSONArray questions = jsonResponse.getJSONArray("questions");
            
            // 保存题目
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                
                // 创建题目实体
                Question questionEntity = new Question();
                questionEntity.setQuestionContent(question.getStr("content"));
                questionEntity.setQuestionType(0); // 默认为单选题
                questionEntity.setOptions(question.getJSONArray("options").toList(String.class));
                questionEntity.setAnswer(Collections.singletonList(question.getStr("answer")));
                questionEntity.setScore(10); // 默认每题10分
                questionEntity.setSource(1); // 标记为AI生成
                questionEntity.setUserId(loginUser.getId());
                
                // 保存题目
                boolean questionSaved = questionService.save(questionEntity);
                if (!questionSaved) {
                    log.error("保存题目失败");
                    continue;
                }
                
                // 创建题目和题单的关联
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(newQuestionBankId);
                questionBankQuestion.setQuestionId(questionEntity.getId());
                questionBankQuestion.setQuestionOrder(i + 1);
                questionBankQuestion.setUserId(loginUser.getId());
                
                questionBankQuestionService.save(questionBankQuestion);
            }
        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            // 如果AI生成失败，使用默认题目作为备用
            createDefaultQuestionsFallback(newQuestionBankId, loginUser.getId());
        }

        return newQuestionBankId;
    }
}




