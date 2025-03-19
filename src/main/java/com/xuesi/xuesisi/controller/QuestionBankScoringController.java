package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.dto.questionbank.SubmitAnswersRequest;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.service.QuestionBankScoringService;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库评分控制器
 */
@RestController
@RequestMapping("/api/question-bank/scoring")
@Slf4j
public class QuestionBankScoringController {

    @Resource
    private QuestionBankScoringService questionBankScoringService;

    @Resource
    private UserService userService;

    @Resource
    private ScoringResultService scoringResultService;

    /**
     * 初始化评分结果
     */
    @PostMapping("/init")
    public BaseResponse<ScoringResultVO> initializeScoringResult(
            @RequestParam Long questionBankId,
            HttpServletRequest request) {
        if (questionBankId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userService.getLoginUser(request).getId();
        ScoringResultVO result = questionBankScoringService.initializeScoringResult(questionBankId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 提交答案并评分
     */
    @PostMapping("/submit")
    public BaseResponse<UserAnswerVO> submitAnswers(
            @RequestBody SubmitAnswersRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || request.getQuestionBankId() == null || request.getAnswers() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long questionBankId = request.getQuestionBankId();
        List<String> answers = request.getAnswers();
        Integer duration = request.getDuration();
        if (questionBankId == null || answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = userService.getLoginUser(httpRequest).getId();
        
        // 更新评分结果的答题用时
        ScoringResult scoringResult = scoringResultService.getOne(
            new LambdaQueryWrapper<ScoringResult>()
                .eq(ScoringResult::getQuestionBankId, questionBankId)
                .eq(ScoringResult::getUserId, userId)
                .orderByDesc(ScoringResult::getCreateTime)
                .last("LIMIT 1")
        );
        
        if (scoringResult != null && duration != null) {
            scoringResult.setDuration(duration);
            scoringResultService.updateById(scoringResult);
        }
        
        UserAnswerVO result = questionBankScoringService.submitAnswers(questionBankId, userId, answers);
        return ResultUtils.success(result);
    }
} 