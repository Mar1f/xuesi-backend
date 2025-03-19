package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.annotation.AuthCheck;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.constant.UserConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.dto.ReviewRequest;
import com.xuesi.xuesisi.model.dto.questionbank.*;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.TeachingPlan;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * 题库接口
 */
@RestController
@RequestMapping("/api/question-bank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

    @Resource
    private TeachingPlanService teachingPlanService;

    /**
     * 管理员审核题库
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doQuestionBankReview(@RequestBody ReviewRequest reviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = reviewRequest.getId();
        Integer reviewStatus = reviewRequest.getReviewStatus();
        // 校验
        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldQuestionBank.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        User loginUser = userService.getLoginUser(request);
        QuestionBank questionBank = new QuestionBank();
        questionBank.setId(id);
        questionBank.setReviewStatus(reviewStatus);
        questionBank.setReviewMessage(reviewRequest.getReviewMessage());
        questionBank.setReviewerId(loginUser.getId());
        questionBank.setReviewTime(new Date());
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 创建题库
     */
    @PostMapping("/create")
    public BaseResponse<Long> createQuestionBank(@RequestBody QuestionBank questionBank, HttpServletRequest request) {
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 校验必要参数
        if (questionBank.getTitle() == null || questionBank.getTitle().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题库标题不能为空");
        }
        
        // 获取当前登录用户ID
        Long userId = userService.getLoginUser(request).getId();
        questionBank.setUserId(userId);
        
        try {
            Long questionBankId = questionBankService.createQuestionBank(questionBank);
            return ResultUtils.success(questionBankId);
        } catch (BusinessException e) {
            log.error("创建题库失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建题库发生未知错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建题库失败：系统错误");
        }
    }

    /**
     * 更新题库
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBank questionBank) {
        if (questionBank == null || questionBank.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = questionBankService.updateQuestionBank(questionBank);
        return ResultUtils.success(result);
    }

    /**
     * 删除题库
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionBank(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = questionBankService.deleteQuestionBank(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取题库列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBanks(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        Page<QuestionBankVO> questionBankPage = questionBankService.listQuestionBanks(current, size);
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 获取题库详情
     */
    @GetMapping("/get")
    public BaseResponse<QuestionBankVO> getQuestionBank(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionBankVO questionBank = questionBankService.getQuestionBankById(id);
        return ResultUtils.success(questionBank);
    }

    /**
     * 获取题库统计信息
     */
    @GetMapping("/stats")
    public BaseResponse<QuestionBankVO> getQuestionBankStats(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionBankVO stats = questionBankService.getQuestionBankStats(id);
        return ResultUtils.success(stats);
    }

    /**
     * 添加题目到题库
     */
    @PostMapping("/addQuestions")
    public BaseResponse<Boolean> addQuestionsToBank(
            @RequestParam Long questionBankId,
            @RequestBody List<Long> questionIds) {
        if (questionBankId == null || questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = questionBankService.addQuestionsToBank(questionBankId, questionIds);
        return ResultUtils.success(result);
    }

    /**
     * 从题库中移除题目
     */
    @PostMapping("/removeQuestions")
    public BaseResponse<Boolean> removeQuestionsFromBank(
            @RequestParam Long questionBankId,
            @RequestBody List<Long> questionIds) {
        if (questionBankId == null || questionIds == null || questionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = questionBankService.removeQuestionsFromBank(questionBankId, questionIds);
        return ResultUtils.success(result);
    }

    /**
     * 获取题库中的题目列表
     */
    @GetMapping("/questions")
    public BaseResponse<List<QuestionVO>> getQuestionsByBankId(@RequestParam Long questionBankId) {
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<QuestionVO> questions = questionBankService.getQuestionsByBankId(questionBankId);
        return ResultUtils.success(questions);
    }

    /**
     * 获取评分历史
     */
    @GetMapping("/scoringHistory")
    public BaseResponse<List<ScoringResultVO>> getScoringHistory(
            @RequestParam Long questionBankId,
            HttpServletRequest request) {
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userService.getLoginUser(request).getId();
        List<ScoringResultVO> history = questionBankService.getScoringHistory(questionBankId, userId);
        return ResultUtils.success(history);
    }
}