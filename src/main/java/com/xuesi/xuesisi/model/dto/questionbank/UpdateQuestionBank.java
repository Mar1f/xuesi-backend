package com.xuesi.xuesisi.model.dto.questionbank;

import lombok.Data;

import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class UpdateQuestionBank {
    private String title;
    private String description;
    private String picture;
    private Integer questionBankType;
    private Integer scoringStrategy;
    private Integer totalScore;
    private Integer passScore;
    private Date endTime;
    private Integer questionCount;
    /**
     * 学科
     */
    private String subject;
    // 如需更新审核状态、审核信息等字段，可在此扩展
}
