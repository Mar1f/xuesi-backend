package com.xuesi.xuesisi.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class GenerateQuestionRequest {
    /**
     * 教师ID
     */
    private Long teacherId;
    /**
     * 用于生成题目的关键词列表
     */
    private List<String> keywords;
}