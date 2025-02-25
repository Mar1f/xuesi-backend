package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import com.xuesi.xuesisi.model.entity.Analysis;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 学习分析视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class AnalysisVO implements Serializable {

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
     * @param analysisVO
     * @return
     */
    public static Analysis voToObj(AnalysisVO analysisVO) {
        if (analysisVO == null) {
            return null;
        }
        Analysis analysis = new Analysis();
        BeanUtils.copyProperties(analysisVO, analysis);
        List<String> tagList = analysisVO.getTagList();
        analysis.setTags(JSONUtil.toJsonStr(tagList));
        return analysis;
    }

    /**
     * 对象转封装类
     *
     * @param analysis
     * @return
     */
    public static AnalysisVO objToVo(Analysis analysis) {
        if (analysis == null) {
            return null;
        }
        AnalysisVO analysisVO = new AnalysisVO();
        BeanUtils.copyProperties(analysis, analysisVO);
        analysisVO.setTagList(JSONUtil.toList(analysis.getTags(), String.class));
        return analysisVO;
    }
}
