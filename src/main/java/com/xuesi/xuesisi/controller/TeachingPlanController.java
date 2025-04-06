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

    /**
     * 获取教学计划
     *
     * @param userAnswerId 用户答题记录ID
     * @return 教学计划
     */
    @GetMapping("/get")
    public BaseResponse<TeachingPlan> getTeachingPlan(@RequestParam Long userAnswerId) {
        log.info("开始获取教案，答题记录ID：{}", userAnswerId);
        if (userAnswerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取答题记录
        UserAnswer userAnswer = userAnswerService.getById(userAnswerId);
        if (userAnswer == null || userAnswer.getIsDelete() != null && userAnswer.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "答题记录不存在");
        }
        log.info("找到答题记录：{}", userAnswer);
        
        // 获取已有教案
        TeachingPlan teachingPlan = teachingPlanService.getByUserAnswerId(userAnswerId);
        if (teachingPlan != null) {
            return ResultUtils.success(teachingPlan);
        }
        
        // 重新生成教案
        log.info("未找到教案，尝试重新生成，答题记录ID：{}", userAnswerId);
        try {
            Long teachingPlanId = teachingPlanService.generateTeachingPlan(userAnswerId);
            if (teachingPlanId != null) {
                teachingPlan = teachingPlanService.getById(teachingPlanId);
                log.info("重新生成教案成功：{}", teachingPlan);
                return ResultUtils.success(teachingPlan);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成教案失败：返回的ID为空");
            }
        } catch (BusinessException e) {
            log.error("重新生成教案失败", e);
            
            // 根据错误类型提供详细的错误信息
            String errorMsg = "生成教案失败：" + e.getMessage();
            if (e.getMessage().contains("Expected a ',' or ']'") || 
                e.getMessage().contains("JSON")) {
                errorMsg = "AI生成的教案格式有误，请稍后重试";
            }
            
            throw new BusinessException(ErrorCode.OPERATION_ERROR, errorMsg);
        } catch (Exception e) {
            log.error("重新生成教案失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成教案失败，服务器内部错误");
        }
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
