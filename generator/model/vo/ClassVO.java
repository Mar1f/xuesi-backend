package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 班级视图
 *
 */
@Data
public class ClassVO implements Serializable {

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
     * @param classVO
     * @return
     */
    public static Class voToObj(ClassVO classVO) {
        if (classVO == null) {
            return null;
        }
        Class class = new Class();
        BeanUtils.copyProperties(classVO, class);
        List<String> tagList = classVO.getTagList();
        class.setTags(JSONUtil.toJsonStr(tagList));
        return class;
    }

    /**
     * 对象转封装类
     *
     * @param class
     * @return
     */
    public static ClassVO objToVo(Class class) {
        if (class == null) {
            return null;
        }
        ClassVO classVO = new ClassVO();
        BeanUtils.copyProperties(class, classVO);
        classVO.setTagList(JSONUtil.toList(class.getTags(), String.class));
        return classVO;
    }
}
