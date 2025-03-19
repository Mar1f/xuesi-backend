package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.scoring.ScoringStrategyExecutor;
import com.xuesi.xuesisi.service.QuestionBankScoringService;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.service.UserAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 题库评分服务实现类
 */
@Service
@Slf4j
public class QuestionBankScoringServiceImpl implements QuestionBankScoringService {

    @Resource
    private ScoringResultService scoringResultService;

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private ScoringStrategyExecutor scoringStrategyExecutor;

    /**
     * 校验用户答案数据
     *
     * @param userAnswer 用户答案
     * @param questionBank 题库信息
     */
    private void validateUserAnswer(UserAnswer userAnswer, QuestionBank questionBank) {
        if (userAnswer == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验题库ID
        Long questionBankId = userAnswer.getQuestionBankId();
        if (questionBankId == null || questionBankId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库ID非法");
        }

        // 校验答案列表
        String choices = userAnswer.getChoices();
        if (StringUtils.isBlank(choices)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案列表不能为空");
        }

        // 校验题库是否存在
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoringResultVO initializeScoringResult(Long questionBankId, Long userId) {
        // 参数校验
        if (questionBankId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 创建新的评分结果
        ScoringResult scoringResult = new ScoringResult();
        scoringResult.setQuestionBankId(questionBankId);
        scoringResult.setUserId(userId);
        scoringResult.setScore(0);
        scoringResult.setDuration(0);
        scoringResult.setStatus(0); // 未完成状态
        scoringResult.setCreateTime(new Date());
        scoringResult.setUpdateTime(new Date());
        scoringResult.setIsDelete(0);

        // 保存评分结果
        boolean saved = scoringResultService.save(scoringResult);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "初始化评分结果失败");
        }

        // 转换为VO并返回
        return ScoringResultVO.objToVo(scoringResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAnswerVO submitAnswers(Long questionBankId, Long userId, List<String> answers) {
        // 参数校验
        if (questionBankId == null || userId == null || answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        try {
            // 创建题库对象（只包含必要信息）
            QuestionBank questionBank = new QuestionBank();
            questionBank.setId(questionBankId);
            questionBank.setUserId(userId);
            questionBank.setScoringStrategy(1); // 使用AI评分策略

            // 执行评分
            UserAnswer userAnswer = scoringStrategyExecutor.doScore(answers, questionBank);
            
            // 更新评分结果
            ScoringResult scoringResult = scoringResultService.getOne(
                new LambdaQueryWrapper<ScoringResult>()
                    .eq(ScoringResult::getQuestionBankId, questionBankId)
                    .eq(ScoringResult::getUserId, userId)
                    .orderByDesc(ScoringResult::getCreateTime)
                    .last("LIMIT 1")
            );
            
            if (scoringResult != null) {
                scoringResult.setScore(userAnswer.getResultScore());
                scoringResult.setStatus(1); // 设置为已完成
                scoringResultService.updateById(scoringResult);
            }
            
            // 保存用户答案
            userAnswerService.save(userAnswer);

            // 转换为VO并返回
            return UserAnswerVO.objToVo(userAnswer);
        } catch (Exception e) {
            log.error("提交答案失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交答案失败：" + e.getMessage());
        }
    }
} 