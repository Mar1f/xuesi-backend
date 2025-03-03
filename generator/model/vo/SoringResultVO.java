package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import com.xuesi.xuesisi.model.entity.SoringResult;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评分结果视图
 *
 */
@Data
public class SoringResultVO implements Serializable {

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
     * @param soringResultVO
     * @return
     */
    public static SoringResult voToObj(SoringResultVO soringResultVO) {
        if (soringResultVO == null) {
            return null;
        }
        SoringResult soringResult = new SoringResult();
        BeanUtils.copyProperties(soringResultVO, soringResult);
        List<String> tagList = soringResultVO.getTagList();
        soringResult.setTags(JSONUtil.toJsonStr(tagList));
        return soringResult;
    }

    /**
     * 对象转封装类
     *
     * @param soringResult
     * @return
     */
    public static SoringResultVO objToVo(SoringResult soringResult) {
        if (soringResult == null) {
            return null;
        }
        SoringResultVO soringResultVO = new SoringResultVO();
        BeanUtils.copyProperties(soringResult, soringResultVO);
        soringResultVO.setTagList(JSONUtil.toList(soringResult.getTags(), String.class));
        return soringResultVO;
    }
}
