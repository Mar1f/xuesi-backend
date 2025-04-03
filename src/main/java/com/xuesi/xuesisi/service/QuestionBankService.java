package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;

import java.util.List;
import java.util.Map;

/**
 * 题库服务接口
 */
public interface QuestionBankService extends IService<QuestionBank> {

    /**
     * 创建题库
     *
     * @param questionBank 题库信息
     * @return 题库ID
     */
    Long createQuestionBank(QuestionBank questionBank);

    /**
     * 更新题库
     *
     * @param questionBank 题库信息
     * @return 是否更新成功
     */
    boolean updateQuestionBank(QuestionBank questionBank);

    /**
     * 删除题库
     *
     * @param id 题库ID
     * @return 是否删除成功
     */
    boolean deleteQuestionBank(Long id);

    /**
     * 获取题库详情
     *
     * @param id 题库ID
     * @return 题库详情
     */
    QuestionBankVO getQuestionBankById(Long id);

    /**
     * 分页查询题库列表
     *
     * @param current 当前页码
     * @param size    每页大小
     * @return 题库列表
     */
    Page<QuestionBankVO> listQuestionBanks(long current, long size);

    /**
     * 添加题目到题库
     *
     * @param questionBankId 题库ID
     * @param questionIds    题目ID列表
     * @return 是否添加成功
     */
    boolean addQuestionsToBank(Long questionBankId, List<Long> questionIds);

    /**
     * 从题库中移除题目
     *
     * @param questionBankId 题库ID
     * @param questionIds    题目ID列表
     * @return 是否移除成功
     */
    boolean removeQuestionsFromBank(Long questionBankId, List<Long> questionIds);

    /**
     * 获取题库中的题目列表
     *
     * @param questionBankId 题库ID
     * @return 题目列表
     */
    List<QuestionVO> getQuestionsByBankId(Long questionBankId);

    /**
     * 获取用户的评分历史
     *
     * @param questionBankId 题库ID
     * @param userId        用户ID
     * @return 评分历史列表
     */
    List<ScoringResultVO> getScoringHistory(Long questionBankId, Long userId);

    /**
     * 获取题库的统计信息
     *
     * @param questionBankId 题库ID
     * @return 统计信息
     */
    QuestionBankVO getQuestionBankStats(Long questionBankId);

    /**
     * 获取学情分析
     *
     * @param questionBankId 题库ID
     * @param userId        用户ID
     * @return 学情分析数据
     */
    Map<String, Object> getLearningAnalysis(Long questionBankId, Long userId);
}