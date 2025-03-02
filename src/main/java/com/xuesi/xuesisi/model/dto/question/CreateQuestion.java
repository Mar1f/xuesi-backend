package com.xuesi.xuesisi.model.dto.question;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class CreateQuestion {
    /**
     * 题干文本
     */
    private String questionContent;
    /**
     * 标签列表，例如 ["代数", "几何"]
     */
    private List<String> tags;
    /**
     * 题型：0-单选, 1-多选, 2-填空
     */
    private Integer questionType;
    /**
     * 选项列表，例如 ["A", "B", "C", "D"]
     */
    private List<String> options;
    /**
     * 正确答案
     */
    private String answer;
    /**
     * 题目分值
     */
    private Integer score;
    /**
     * 来源：0-手动, 1-AI生成
     */
    private Integer source;
    /**
     * 创建人ID（教师或管理员）
     */
    private Long userId;
}
