package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 学生答案视图
 *
 */
@Data
public class UserAnswerVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建用户 id
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
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param userAnswerVO
     * @return
     */
    public static UserAnswer voToObj(UserAnswerVO userAnswerVO) {
        if (userAnswerVO == null) {
            return null;
        }
        UserAnswer userAnswer = new UserAnswer();
        BeanUtils.copyProperties(userAnswerVO, userAnswer);
        List<String> tagList = userAnswerVO.getTagList();
        userAnswer.setTags(JSONUtil.toJsonStr(tagList));
        return userAnswer;
    }

    /**
     * 对象转封装类
     *
     * @param userAnswer
     * @return
     */
    public static UserIdVO objToVo(UserId userAnswer) {
        if (userAnswer == null) {
            return null;
        }
        UserIdVO userAnswerVO = new UserIdVO();
        BeanUtils.copyProperties(userAnswer, userAnswerVO);
        userAnswerVO.setTagList(JSONUtil.toList(userAnswer.getTags(), String.class));
        return userAnswerVO;
    }
}
