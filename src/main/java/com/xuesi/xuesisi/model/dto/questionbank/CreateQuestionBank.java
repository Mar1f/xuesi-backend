package com.xuesi.xuesisi.model.dto.questionbank;

import lombok.Data;

import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
public class CreateQuestionBank {
    @Data
    public class CreateQuestionBankDto {
        private String title;
        private String description;
        private String picture;
        /**
         * 题单类型：0-得分类, 1-测评类
         */
        private Integer questionBankType;
        /**
         * 评分策略：0-自定义, 1-AI
         */
        private Integer scoringStrategy;
        /**
         * 题单总分
         */
        private Integer totalScore;
        /**
         * 及格分
         */
        private Integer passScore;
        /**
         * 截止时间
         */
        private Date endTime;
        /**
         * 创建人ID
         */
        private Long userId;
        /**
         * 题目数量
         */
        private Integer questionCount;
        /**
         * 学科
         */
        private String subject;
    }
}