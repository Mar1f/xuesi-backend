package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.question.CreateQuestionRequest;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.service.QuestionBankQuestionService;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.mapper.QuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
* @author mar1
* @description 针对表【question(题目表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Override
    public Question createQuestion(Question question) {
        save(question);
        return question;
    }

    @Override
    public Question getQuestionById(Long id) {
        return getById(id);
    }

    @Override
    public List<Question> getAllQuestions() {
        return list();
    }

    @Override
    public Question updateQuestion(Long id, Question question) {
        question.setId(id);
        if (updateById(question)) {
            return getById(id);
        }
        return null;
    }

    @Override
    public void deleteQuestion(Long id) {
        removeById(id);
    }

    @Override
    public Page<Question> getQuestionsPage(int pageNumber, int pageSize) {
        Page<Question> page = new Page<>(pageNumber, pageSize);
        return page(page);
    }

    /**
     * 利用 Deepseek 接口生成题目（伪代码示例）
     *
     * @param teacherId 教师ID
     * @param keywords  关键词列表
     * @return 生成的题目列表
     */
    @Override
    public List<Question> generateQuestions(Long teacherId, List<String> keywords) {
        // TODO: 替换下面的伪代码为 Deepseek 的实际调用代码
        List<Question> generatedQuestions = deepseekGenerate(keywords);
        // 标记题目为 AI 生成并设置创建人
        for (Question q : generatedQuestions) {
            q.setUserId(teacherId);
            q.setSource(1); // 1 表示 AI 生成
            save(q);
        }
        return generatedQuestions;
    }

    // 模拟调用 Deepseek 生成题目的方法，实际请使用 HTTP 客户端调用 Deepseek API
    private List<Question> deepseekGenerate(List<String> keywords) {
        // 例如：调用 deepseekClient.generateQuestions(keywords);
        // 这里直接返回一个空列表作为示例
        return Collections.emptyList(); // 返回空列表
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question createQuestionWithBinding(CreateQuestionRequest createQuestionRequest, Long userId) {
        if (createQuestionRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 创建题目
        Question question = new Question();
        question.setQuestionContent(createQuestionRequest.getQuestionContent());
        question.setTags(createQuestionRequest.getTags());
        question.setQuestionType(createQuestionRequest.getQuestionType());
        question.setOptions(createQuestionRequest.getOptions());
        question.setAnswer(createQuestionRequest.getAnswer());
        question.setScore(createQuestionRequest.getScore());
        question.setSource(createQuestionRequest.getSource());
        question.setUserId(userId);

        boolean saveResult = this.save(question);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目保存失败");
        }

        // 2. 如果有题单绑定信息，创建题目和题单的关联
        CreateQuestionRequest.QuestionBankBinding binding = createQuestionRequest.getQuestionBankBinding();
        if (binding != null && binding.getQuestionBankId() != null) {
            QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
            questionBankQuestion.setQuestionBankId(binding.getQuestionBankId());
            questionBankQuestion.setQuestionId(question.getId());
            questionBankQuestion.setQuestionOrder(binding.getQuestionOrder());
            questionBankQuestion.setUserId(userId);

            boolean bindResult = questionBankQuestionService.save(questionBankQuestion);
            if (!bindResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目绑定题单失败");
            }
        }

        return question;
    }
}



