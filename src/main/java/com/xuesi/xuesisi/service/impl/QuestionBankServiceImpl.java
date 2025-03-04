package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author mar1
* @description 针对表【question_bank(题单表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    @Override
    public QuestionBank createQuestionBank(QuestionBank questionBank) {
        save(questionBank);
        return questionBank;
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
}




