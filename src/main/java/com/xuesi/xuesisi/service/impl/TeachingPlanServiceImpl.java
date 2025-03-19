package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.TeachingPlanMapper;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 教学计划服务实现类
 */
@Service
@Slf4j
public class TeachingPlanServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan> implements TeachingPlanService {

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private TeachingPlanGenerationService teachingPlanGenerationService;

    @Override
    public TeachingPlan getByUserAnswerId(Long userAnswerId) {
        return getOne(new LambdaQueryWrapper<TeachingPlan>()
                .eq(TeachingPlan::getUserAnswerId, userAnswerId)
                .orderByDesc(TeachingPlan::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    public TeachingPlan getLatestByQuestionBankId(Long questionBankId) {
        return getOne(new LambdaQueryWrapper<TeachingPlan>()
                .eq(TeachingPlan::getQuestionBankId, questionBankId)
                .orderByDesc(TeachingPlan::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    public Long generateTeachingPlan(Long userAnswerId) {
        // 1. 获取用户答题记录
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null) {
            log.error("用户答题记录不存在，userAnswerId: {}", userAnswerId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户答题记录不存在");
        }

        // 2. 获取题库信息
        QuestionBank questionBank = questionBankService.getById(userAnswer.getQuestionBankId());
        if (questionBank == null) {
            log.error("题库不存在，questionBankId: {}", userAnswer.getQuestionBankId());
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }

        // 3. 生成教学计划
        UserAnswerVO userAnswerVO = UserAnswerVO.objToVo(userAnswer);
        TeachingPlan teachingPlan = teachingPlanGenerationService.generateTeachingPlan(
            questionBank,
            userAnswerVO,
            userAnswer.getResultDesc()
        );

        // 4. 保存教学计划
        boolean success = save(teachingPlan);
        if (!success) {
            log.error("保存教学计划失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存教学计划失败");
        }

        return teachingPlan.getId();
    }
}
