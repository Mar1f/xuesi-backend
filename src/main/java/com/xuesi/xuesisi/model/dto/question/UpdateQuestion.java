package com.xuesi.xuesisi.model.dto.question;

import lombok.Data;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class UpdateQuestion {
    private String questionContent;
    private List<String> tags;
    private Integer questionType;
    private List<String> options;
    private String answer;
    private Integer score;
}
