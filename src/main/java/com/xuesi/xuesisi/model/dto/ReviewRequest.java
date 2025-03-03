package com.xuesi.xuesisi.model.dto;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/03
 **/
@Data
public class ReviewRequest {
    private Long id; // 题目或题单的ID
    private Integer reviewStatus; // 审核状态 (0-待审, 1-通过, 2-拒绝)
    private String reviewMessage; // 审核信息
}
