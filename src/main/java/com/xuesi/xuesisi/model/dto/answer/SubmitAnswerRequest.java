package com.xuesi.xuesisi.model.dto.answer;

import lombok.Data;
import java.util.List;

@Data
public class SubmitAnswerRequest {
    /**
     * 题单ID
     */
    private Long questionBankId;

    /**
     * 答案列表
     */
    private List<QuestionAnswer> choices;

    @Data
    public static class QuestionAnswer {
        /**
         * 题目ID
         */
        private Long questionId;

        /**
         * 答案内容
         */
        private List<String> answer;
    }
} 