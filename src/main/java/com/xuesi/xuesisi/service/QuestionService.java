package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
