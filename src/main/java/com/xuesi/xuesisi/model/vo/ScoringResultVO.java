package com.xuesi.xuesisi.model.vo;

import com.xuesi.xuesisi.model.entity.ScoringResult;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 评分结果视图对象
 */
@Data
public class ScoringResultVO implements Serializable {
    /**
     * 评分结果ID
     */
    private Long id;

    /**
     * 题库ID
     */
    private Long questionBankId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 答题用时（秒）
     */
    private Integer duration;

    /**
     * 答题状态（0-未完成，1-已完成）
     */
    private Integer status;

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
     * 对象转封装类
     *
     * @param scoringResult
     * @return
     */
    public static ScoringResultVO objToVo(ScoringResult scoringResult) {
        if (scoringResult == null) {
            return null;
        }
        ScoringResultVO scoringResultVO = new ScoringResultVO();
        BeanUtils.copyProperties(scoringResult, scoringResultVO);
        return scoringResultVO;
    }
}
