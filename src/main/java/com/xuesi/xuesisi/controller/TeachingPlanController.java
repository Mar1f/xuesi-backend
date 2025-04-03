package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.service.TeachingPlanService;
import com.xuesi.xuesisi.service.UserAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    /**
     * 获取所有教案
     */
    @GetMapping("/list")
    public BaseResponse<List<TeachingPlan>> getAllTeachingPlans(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long questionBankId) {
        log.info("开始获取教案列表，用户ID：{}，题库ID：{}", userId, questionBankId);
        List<TeachingPlan> plans = teachingPlanService.list(userId, questionBankId);
        return ResultUtils.success(plans);
    }

    /**
     * 分页获取教案
     */
    @GetMapping("/page")
    public BaseResponse<Page<TeachingPlan>> getTeachingPlanPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long questionBankId) {
        log.info("开始分页获取教案，当前页：{}，每页大小：{}，用户ID：{}，题库ID：{}", 
                current, size, userId, questionBankId);
        Page<TeachingPlan> page = teachingPlanService.page(current, size, userId, questionBankId);
        return ResultUtils.success(page);
    }

    /**
     * 创建教案
     */
    @PostMapping("/create")
    public BaseResponse<TeachingPlan> createTeachingPlan(@RequestBody TeachingPlan teachingPlan) {
        if (teachingPlan == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("开始创建教案：{}", teachingPlan);
        TeachingPlan result = teachingPlanService.createTeachingPlan(teachingPlan);
        return ResultUtils.success(result);
    }

    /**
     * 更新教案
     */
    @PostMapping("/update")
    public BaseResponse<TeachingPlan> updateTeachingPlan(@RequestBody TeachingPlan teachingPlan) {
        if (teachingPlan == null || teachingPlan.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("开始更新教案：{}", teachingPlan);
        TeachingPlan result = teachingPlanService.updateTeachingPlan(teachingPlan);
        return ResultUtils.success(result);
    }

    /**
     * 删除教案
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeachingPlan(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("开始删除教案，ID：{}", id);
        boolean result = teachingPlanService.removeById(id);
        return ResultUtils.success(result);
    }
}
