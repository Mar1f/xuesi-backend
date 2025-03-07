package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.QuestionBankMapper;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionBankQuestion;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.service.QuestionBankService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题库服务实现类
 */
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestionBank(QuestionBank questionBank) {
        // 参数校验
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 设置默认值
        questionBank.setQuestionCount(0);
        // 保存题库
        boolean success = save(questionBank);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return questionBank.getId();
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




