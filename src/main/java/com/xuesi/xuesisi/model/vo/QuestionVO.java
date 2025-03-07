package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    private Integer type;

    /**
     * 选项列表
     */
    private List<String> options;

    /**
     * 答案
     */
    private List<String> answers;

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
    private Long creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

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
    private Boolean isDelete;

    /**
     * 题目内容列表
     */
    private List<QuestionContentDTO> questionContent;

    /**
     * 获取题目内容列表
     */
    public List<QuestionContentDTO> getQuestionContent() {
        return questionContent;
    }

    /**
     * 设置题目内容列表
     */
    public void setQuestionContent(List<QuestionContentDTO> questionContent) {
        this.questionContent = questionContent;
    }

    /**
     * 封装类转对象
     *
     * @param questionVO
     * @return
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);
        List<QuestionContentDTO> questionContentDTO = questionVO.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        return question;
    }

    /**
     * 对象转封装类
     *
     * @param question
     * @return
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO);
        String questionContent = question.getQuestionContent();
        if (questionContent != null) {
            questionVO.setQuestionContent(JSONUtil.toList(questionContent, QuestionContentDTO.class));
        }
        return questionVO;
    }
}
