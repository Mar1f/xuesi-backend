package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.annotation.AuthCheck;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.constant.UserConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.dto.ReviewRequest;
import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.service.QuestionService;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Resource
    private QuestionService questionService;
    @Resource
    private UserService userService;
    // 创建题目
    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question createdQuestion = questionService.createQuestion(question);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    // 根据ID查询题目
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        if (question != null) {
            return ResponseEntity.ok(question);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有题目
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }

    // 修改题目
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        Question updatedQuestion = questionService.updateQuestion(id, question);
        if (updatedQuestion != null) {
            return ResponseEntity.ok(updatedQuestion);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除题目
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
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