package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.TeachingPlanMapper;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.QuestionScoringResult;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

/**
 * 教学计划服务实现类
 */
@Service
@Slf4j
public class TeachingPlanServiceImpl extends ServiceImpl<TeachingPlanMapper, TeachingPlan> implements TeachingPlanService {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserAnswerService userAnswerService;

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
        if (userAnswerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 1. 获取答题记录
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答题记录不存在");
        }
        
        // 2. 获取题库信息
        QuestionBank questionBank = questionBankService.getById(userAnswer.getQuestionBankId());
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
        
        // 3. 构建评分结果
        QuestionScoringResult scoringResult = new QuestionScoringResult();
        scoringResult.setQuestionId(userAnswer.getId());
        scoringResult.setQuestionType(userAnswer.getQuestionBankType());
        scoringResult.setUserAnswer(userAnswer.getChoices());
        scoringResult.setScore(userAnswer.getResultScore());
        scoringResult.setAnalysis(userAnswer.getResultDesc());
        List<QuestionScoringResult> scoringResults = Collections.singletonList(scoringResult);
        
        // 4. 调用生成服务
        try {
            TeachingPlan teachingPlan = teachingPlanGenerationService.generateTeachingPlan(
                    questionBank, scoringResults, userAnswerId
            );
            
            // 5. 返回教学计划ID
            if (teachingPlan != null && teachingPlan.getId() != null) {
                return teachingPlan.getId();
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成教学计划失败");
            }
        } catch (BusinessException e) {
            // 保留业务异常的详细信息，直接向上抛出
            throw e;
        } catch (Exception e) {
            // 包装其他异常为业务异常
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成教学计划失败：" + e.getMessage());
        }
    }

    @Override
    public List<TeachingPlan> list(Long userId, Long questionBankId) {
        LambdaQueryWrapper<TeachingPlan> queryWrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            queryWrapper.eq(TeachingPlan::getUserId, userId);
        }
        if (questionBankId != null) {
            queryWrapper.eq(TeachingPlan::getQuestionBankId, questionBankId);
        }
        
        queryWrapper.orderByDesc(TeachingPlan::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public Page<TeachingPlan> page(long current, long size, Long userId, Long questionBankId) {
        LambdaQueryWrapper<TeachingPlan> queryWrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            queryWrapper.eq(TeachingPlan::getUserId, userId);
        }
        if (questionBankId != null) {
            queryWrapper.eq(TeachingPlan::getQuestionBankId, questionBankId);
        }
        
        queryWrapper.orderByDesc(TeachingPlan::getCreateTime);
        return page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public TeachingPlan createTeachingPlan(TeachingPlan teachingPlan) {
        if (teachingPlan == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 验证必要字段
        Long questionBankId = teachingPlan.getQuestionBankId();
        if (teachingPlan.getUserId() == null || questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID和题库ID不能为空");
        }

        // 获取题库信息以填充 subject 和 title
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联的题库不存在，ID: " + questionBankId);
        }
        // Set subject and title from QuestionBank
        teachingPlan.setSubject(questionBank.getSubject()); // Assuming subject is also set here
        teachingPlan.setTitle(questionBank.getTitle()); // Corrected: Use getTitle()

        // 保存教案
        boolean success = save(teachingPlan);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建教案失败");
        }

        return teachingPlan;
    }

    @Override
    public TeachingPlan updateTeachingPlan(TeachingPlan teachingPlan) {
        if (teachingPlan == null || teachingPlan.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 验证教案是否存在
        TeachingPlan existingPlan = getById(teachingPlan.getId());
        if (existingPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "教案不存在");
        }

        // 检查是否需要更新 subject 和 title (e.g., if questionBankId is part of the update)
        Long questionBankId = teachingPlan.getQuestionBankId();
        if (questionBankId != null && !questionBankId.equals(existingPlan.getQuestionBankId())) {
            // If questionBankId changed, fetch new QuestionBank and update title/subject
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            if (questionBank == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联的题库不存在，ID: " + questionBankId);
            }
            teachingPlan.setSubject(questionBank.getSubject());
            teachingPlan.setTitle(questionBank.getTitle()); // Corrected: Use getTitle()
        } else if (teachingPlan.getTitle() == null && existingPlan.getQuestionBankId() != null) {
            // If title is not provided in update payload, but bank exists, ensure it's set
             QuestionBank questionBank = questionBankService.getById(existingPlan.getQuestionBankId());
             if (questionBank != null) {
                 teachingPlan.setTitle(questionBank.getTitle()); // Corrected: Use getTitle()
                 // Optionally set subject too if needed
                 if (teachingPlan.getSubject() == null) {
                     teachingPlan.setSubject(questionBank.getSubject());
                 }
             }
        }

        // 更新教案
        boolean success = updateById(teachingPlan);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新教案失败");
        }

        // 返回更新后的完整对象 (updateById might not return the full updated object depending on config)
        // It's safer to return getById(teachingPlan.getId()) or ensure teachingPlan object has all fields
        return getById(teachingPlan.getId()); // Return the fresh object from DB
    }
}
