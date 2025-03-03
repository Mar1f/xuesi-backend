package com.xuesi.xuesisi.controller;



import com.xuesi.xuesisi.annotation.AuthCheck;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.constant.UserConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.dto.ReviewRequest;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.ReviewStatusEnum;
import com.xuesi.xuesisi.service.QuestionBankService;
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
@RequestMapping("/questionBanks")
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;


    // 创建题单
    @PostMapping
    public ResponseEntity<QuestionBank> createQuestionBank(@RequestBody QuestionBank questionBank) {
        QuestionBank createdQuestionBank = questionBankService.createQuestionBank(questionBank);
        return new ResponseEntity<>(createdQuestionBank, HttpStatus.CREATED);
    }

    // 根据ID查询题单
    @GetMapping("/{id}")
    public ResponseEntity<QuestionBank> getQuestionBank(@PathVariable Long id) {
        QuestionBank questionBank = questionBankService.getQuestionBankById(id);
        if (questionBank != null) {
            return ResponseEntity.ok(questionBank);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有题单
    @GetMapping
    public ResponseEntity<List<QuestionBank>> getAllQuestionBanks() {
        List<QuestionBank> questionBanks = questionBankService.getAllQuestionBanks();
        return ResponseEntity.ok(questionBanks);
    }

    // 修改题单
    @PutMapping("/{id}")
    public ResponseEntity<QuestionBank> updateQuestionBank(@PathVariable Long id, @RequestBody QuestionBank questionBank) {
        QuestionBank updatedQuestionBank = questionBankService.updateQuestionBank(id, questionBank);
        if (updatedQuestionBank != null) {
            return ResponseEntity.ok(updatedQuestionBank);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除题单
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable Long id) {
        questionBankService.deleteQuestionBank(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * 管理员审核题单
     * @param reviewRequest
     * @param request
     * @return
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
        questionBank.setReviewerId(loginUser.getId());
        questionBank.setReviewTime(new Date());
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

}