package com.xuesi.xuesisi.model.dto.questionbank;

import lombok.Data;

import java.util.List;

/**
 * 提交答案请求
 */
@Data
public class SubmitAnswersRequest {
    
    /**
     * 题库ID
     */
    private Long questionBankId;
    
    /**
     * 答案列表
     */
    private List<String> answers;

    /**
     * 答题用时（秒）
     */
    private Integer duration;
} 