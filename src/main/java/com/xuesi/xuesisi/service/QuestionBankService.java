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
    /**
     * 校验数据
     *
     * @param questionBank
     * @param add           对创建的数据进行校验
     */
    void validQuestionBank(QuestionBank questionBank, boolean add);

    /**
     * 创建题单
     *
     * @param questionBank
     * @return
     */
    QuestionBank createQuestionBank(QuestionBank questionBank);

    /**
     * 根据ID获取题单
     *
     * @param id
     * @return
     */
    QuestionBank getQuestionBankById(Long id);

    /**
     * 获取所有题单
     *
     * @return
     */
    List<QuestionBank> getAllQuestionBanks();

    /**
     * 更新题单
     *
     * @param id
     * @param questionBank
     * @return
     */
    QuestionBank updateQuestionBank(Long id, QuestionBank questionBank);

    /**
     * 删除题单
     *
     * @param id
     */
    void deleteQuestionBank(Long id);

    /**
     * 分页获取题单
     *
     * @param pageNumber
     * @param pageSize
     * @return
     */
    Page<QuestionBank> getQuestionBanksPage(int pageNumber, int pageSize);
    
    /**
     * 为题库初始化评分结果
     *
     * @param questionBank
     */
    void initScoringResults(QuestionBank questionBank);

    /**
     * 添加题单
     *
     * @param questionBank
     * @return
     */
    long addQuestionBank(QuestionBank questionBank);
}
