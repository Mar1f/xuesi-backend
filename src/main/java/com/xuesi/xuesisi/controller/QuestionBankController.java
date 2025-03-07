package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.vo.QuestionBankVO;
import com.xuesi.xuesisi.model.vo.QuestionVO;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库接口
 */
@RestController
@RequestMapping("/questionBank")
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

    /**
     * 创建题库
     */
    @PostMapping("/create")
    public BaseResponse<Long> createQuestionBank(@RequestBody QuestionBank questionBank) {
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long questionBankId = questionBankService.createQuestionBank(questionBank);
        return ResultUtils.success(questionBankId);
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
     * 初始化评分结果
     */
    @PostMapping("/initScoring")
    public BaseResponse<ScoringResultVO> initializeScoringResult(
            @RequestParam Long questionBankId,
            HttpServletRequest request) {
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userService.getLoginUser(request).getId();
        ScoringResultVO result = questionBankService.initializeScoringResult(questionBankId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 提交答案并评分
     */
    @PostMapping("/submit")
    public BaseResponse<ScoringResultVO> submitAnswers(
            @RequestParam Long questionBankId,
            @RequestBody List<String> answers,
            HttpServletRequest request) {
        if (questionBankId == null || answers == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userService.getLoginUser(request).getId();
        ScoringResultVO result = questionBankService.submitAnswers(questionBankId, userId, answers);
        return ResultUtils.success(result);
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