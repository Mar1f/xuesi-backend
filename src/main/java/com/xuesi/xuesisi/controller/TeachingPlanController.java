package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.common.ErrorCode;
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
 * @description；
 * @author:mar1
 * @data:2025/03/15
 **/
@RestController
@RequestMapping("/teachingPlan")
@Slf4j
public class TeachingPlanController {
    @Resource
    private TeachingPlanService teachingPlanService;
    
    @Resource
    private UserAnswerService userAnswerService;

    @GetMapping("/get")
    public BaseResponse<TeachingPlan> getTeachingPlan(@RequestParam Long userAnswerId) {
        log.info("开始获取教案，答题记录ID：{}", userAnswerId);
        
        // 首先验证答题记录是否存在
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null) {
            log.warn("答题记录不存在，ID：{}", userAnswerId);
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "答题记录不存在");
        }
        log.info("找到答题记录：{}", userAnswer);
        
        // 查询教案
        TeachingPlan plan = teachingPlanService.lambdaQuery()
                .eq(TeachingPlan::getUserAnswerId, userAnswerId)
                .eq(TeachingPlan::getIsDelete, 0)
                .one();
                
        if (plan == null) {
            log.warn("未找到教案，答题记录ID：{}", userAnswerId);
            // 尝试重新生成教案
            Long planId = teachingPlanService.generateTeachingPlan(userAnswerId);
            if (planId != null) {
                plan = teachingPlanService.getById(planId);
                log.info("重新生成教案成功：{}", plan);
            } else {
                log.error("重新生成教案失败");
            }
        } else {
            log.info("找到教案：{}", plan);
        }
        
        return ResultUtils.success(plan);
    }
}
