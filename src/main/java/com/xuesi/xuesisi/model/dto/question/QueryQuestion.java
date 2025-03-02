package com.xuesi.xuesisi.model.dto.question;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class QueryQuestion {
    private Long id;
    private String questionContent;
    private List<String> tags;
    private Integer questionType;
    private List<String> options;
    private String answer;
    private Integer score;
    private Integer source;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    private Integer isDelete;
}
