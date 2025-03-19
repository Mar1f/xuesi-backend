package com.xuesi.xuesisi.model.dto.userAnswer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建用户答案请求
 */
@Data
public class UserAnswerAddRequest implements Serializable {

    /**
     * 题单 id
     */
    private Long questionBankId;

    /**
     * 用户答案列表
     */
    private List<QuestionAnswer> choices;

    /**
     * 答题用时（秒）
     */
    private Integer duration;

    @Data
    public static class QuestionAnswer {
        /**
         * 题目ID
         */
        private Long questionId;

        /**
         * 答案内容
         */
        private List<String> answer;
    }

    private static final long serialVersionUID = 1L;
}