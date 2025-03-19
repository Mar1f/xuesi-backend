package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.service.TeachingPlanService;
import com.xuesi.xuesisi.service.UserAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 教学计划控制器
 */
@RestController
@RequestMapping("/api/teaching-plan")
@Slf4j
public class TeachingPlanController {
    @Resource
    private TeachingPlanService teachingPlanService;
    
    @Resource
    private UserAnswerService userAnswerService;

    @GetMapping("/get")
    public BaseResponse<TeachingPlan> getTeachingPlan(@RequestParam Long userAnswerId) {
        if (userAnswerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        log.info("开始获取教案，答题记录ID：{}", userAnswerId);
        
        // 首先验证答题记录是否存在
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null) {
            log.warn("答题记录不存在，ID：{}", userAnswerId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答题记录不存在");
        }
        log.info("找到答题记录：{}", userAnswer);
        
        // 查询教案
        TeachingPlan plan = teachingPlanService.getByUserAnswerId(userAnswerId);
                
        if (plan == null) {
            log.info("未找到教案，尝试重新生成，答题记录ID：{}", userAnswerId);
            try {
                Long planId = teachingPlanService.generateTeachingPlan(userAnswerId);
                if (planId != null) {
                    plan = teachingPlanService.getById(planId);
                    log.info("重新生成教案成功：{}", plan);
                }
            } catch (Exception e) {
                log.error("重新生成教案失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成教案失败：" + e.getMessage());
            }
        } else {
            log.info("找到教案：{}", plan);
        }
        
        if (plan == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取教案失败");
        }
        
        return ResultUtils.success(plan);
    }
}
