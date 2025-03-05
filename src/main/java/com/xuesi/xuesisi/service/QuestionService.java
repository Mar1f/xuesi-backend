package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.dto.question.CreateQuestionRequest;

import java.util.List;

/**
* @author mar1
* @description 针对表【question(题目表)】的数据库操作Service
* @createDate 2025-03-02 15:47:32
*/
public interface QuestionService extends IService<Question> {
    Question createQuestion(Question question);
    Question getQuestionById(Long id);
    List<Question> getAllQuestions();
    Question updateQuestion(Long id, Question question);
    void deleteQuestion(Long id);
    Page<Question> getQuestionsPage(int pageNumber, int pageSize);
    List<Question> generateQuestions(Long teacherId, List<String> keywords);
    /**
     * 创建题目并绑定题单
     * @param createQuestionRequest 创建题目请求，包含题目信息和题单绑定信息
     * @param userId 创建人ID
     * @return 创建的题目
     */
    Question createQuestionWithBinding(CreateQuestionRequest createQuestionRequest, Long userId);
}
