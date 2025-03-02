package com.xuesi.xuesisi.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class SubmitAnswerRequest {
    /**
     * 题单ID
     */
    private Long questionBankId;
    /**
     * 学生ID
     */
    private Long userId;
    /**
     * 答题详情列表
     */
    private List<AnswerDetailDto> answerDetails;

    @Data
    public static class AnswerDetailDto {
        /**
         * 题目ID
         */
        private Long questionId;
        /**
         * 用户选择的答案
         */
        private String userChoice;
    }
}