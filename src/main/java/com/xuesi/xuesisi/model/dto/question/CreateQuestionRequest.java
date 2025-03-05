package com.xuesi.xuesisi.model.dto.question;

import lombok.Data;
import java.util.List;

@Data
public class CreateQuestionRequest {
    /**
     * 题干文本
     */
    private String questionContent;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题型: 0-单选, 1-多选, 2-填空
     */
    private Integer questionType;

    /**
     * 选项列表
     */
    private List<String> options;

    /**
     * 正确答案
     */
    private List<String> answer;

    /**
     * 题目分值
     */
    private Integer score;

    /**
     * 来源: 0-手动, 1-AI生成
     */
    private Integer source;

    /**
     * 题单绑定信息
     */
    private QuestionBankBinding questionBankBinding;

    @Data
    public static class QuestionBankBinding {
        /**
         * 题单ID
         */
        private Long questionBankId;
        
        /**
         * 题目顺序（题号）
         */
        private Integer questionOrder;
    }
} 