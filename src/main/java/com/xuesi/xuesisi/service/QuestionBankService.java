package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author mar1
* @description 针对表【question_bank(题单表)】的数据库操作Service
* @createDate 2025-03-02 15:47:32
*/
public interface QuestionBankService extends IService<QuestionBank> {
    QuestionBank createQuestionBank(QuestionBank questionBank);
    QuestionBank getQuestionBankById(Long id);
    List<QuestionBank> getAllQuestionBanks();
    QuestionBank updateQuestionBank(Long id, QuestionBank questionBank);
    void deleteQuestionBank(Long id);
    Page<QuestionBank> getQuestionBanksPage(int pageNumber, int pageSize);
}
