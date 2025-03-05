package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.annotation.AuthCheck;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.constant.UserConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.dto.ReviewRequest;
import com.xuesi.xuesisi.model.dto.question.CreateQuestionRequest;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/api/question")
@CrossOrigin(origins = {"http://localhost:8000"}, allowCredentials = "true")
public class QuestionController {

    @Resource
    private QuestionService questionService;
    
    @Resource
    private UserService userService;

    /**
     * 创建题目
     *
     * @param createQuestionRequest 创建题目请求
     * @param request HTTP请求
     * @return 创建的题目
     */
    @PostMapping("/add")
    public BaseResponse<Question> createQuestion(@RequestBody CreateQuestionRequest createQuestionRequest,
                                               HttpServletRequest request) {
        if (createQuestionRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // TODO: 从请求中获取当前登录用户ID，这里暂时写死为1L
        Long userId = 1L;
        Question question = questionService.createQuestionWithBinding(createQuestionRequest, userId);
        return ResultUtils.success(question);
    }

    // 根据ID查询题目
    @GetMapping("/get/{id}")
    public BaseResponse<Question> getQuestion(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(question);
    }

    // 查询所有题目
    @GetMapping("/list")
    public BaseResponse<List<Question>> getAllQuestions() {
        List<Question> questions = questionService.getAllQuestions();
        return ResultUtils.success(questions);
    }

    // 修改题目
    @PostMapping("/update")
    public BaseResponse<Question> updateQuestion(@RequestParam Long id, @RequestBody Question question) {
        if (id == null || question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question updatedQuestion = questionService.updateQuestion(id, question);
        if (updatedQuestion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(updatedQuestion);
    }

    // 删除题目
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        questionService.deleteQuestion(id);
        return ResultUtils.success(true);
    }

//
//    /**
//     * 管理员审核题
//     * @param reviewRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/review")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Boolean> doQuestionReview(@RequestBody ReviewRequest reviewRequest, HttpServletRequest request) {
//        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
//        Long id = reviewRequest.getId();
//        Integer reviewStatus = reviewRequest.getReviewStatus();
//        // 校验
//        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
//        if (id == null || reviewStatusEnum == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // 判断是否存在
//        Question oldQuestion = questionService.getById(id);
//        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
//        // 已是该状态
//        if (oldQuestion.getReviewStatus().equals(reviewStatus)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
//        }
//        // 更新审核状态
//        User loginUser = userService.getLoginUser(request);
//        Question question = new Question();
//        question.setId(id);
//        question.setReviewStatus(reviewStatus);
//        question.setReviewerId(loginUser.getId());
//        question.setReviewTime(new Date());
//        boolean result = questionService.updateById(question);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }
}