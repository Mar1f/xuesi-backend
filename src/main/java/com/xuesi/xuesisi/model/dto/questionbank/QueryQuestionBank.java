package com.xuesi.xuesisi.model.dto.questionbank;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class QueryQuestionBank {
    private Long id;
    private String title;
    private String description;
    private String picture;
    private Integer questionBankType;
    private Integer scoringStrategy;
    private Integer totalScore;
    private Integer passScore;
    private Date endTime;
    /**
     * 审核状态：0-待审, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;
    private String reviewMessage;
    private Long reviewerId;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
}
