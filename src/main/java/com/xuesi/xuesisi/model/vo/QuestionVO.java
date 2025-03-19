package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import com.xuesi.xuesisi.model.dto.question.QuestionContentDTO;
import com.xuesi.xuesisi.model.entity.Question;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题目视图对象
 */
@Data
public class QuestionVO implements Serializable {
    /**
     * 题目ID
     */
    private Long id;

    /**
     * 题目内容
     */
    private String content;

    /**
     * 题目类型
     */
    private Integer questionType;

    /**
     * 选项列表
     */
    private List<String> options;

    /**
     * 答案
     */
    private List<String> answer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 分值
     */
    private Integer score;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建者ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 题目顺序
     */
    private Integer questionOrder;

    /**
     * 参考答案（用于简答题）
     */
    private String referenceAnswer;

    /**
     * 封装类转对象
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);
        question.setQuestionContent(questionVO.getContent());
        return question;
    }

    /**
     * 对象转封装类
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO);
        questionVO.setContent(question.getQuestionContent());
        questionVO.setTags(JSONUtil.toList(question.getTagsStr(), String.class));
        questionVO.setOptions(question.getOptions());
        questionVO.setAnswer(question.getAnswer());
        return questionVO;
    }
}
