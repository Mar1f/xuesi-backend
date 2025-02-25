package com.xuesi.xuesisi.model.vo;

import cn.hutool.json.JSONUtil;
import com.xuesi.xuesisi.model.entity.Assignment;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题单视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class AssignmentVO implements Serializable {

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
     * @param assignmentVO
     * @return
     */
    public static Assignment voToObj(AssignmentVO assignmentVO) {
        if (assignmentVO == null) {
            return null;
        }
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(assignmentVO, assignment);
        List<String> tagList = assignmentVO.getTagList();
        assignment.setTags(JSONUtil.toJsonStr(tagList));
        return assignment;
    }

    /**
     * 对象转封装类
     *
     * @param assignment
     * @return
     */
    public static AssignmentVO objToVo(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        AssignmentVO assignmentVO = new AssignmentVO();
        BeanUtils.copyProperties(assignment, assignmentVO);
        assignmentVO.setTagList(JSONUtil.toList(assignment.getTags(), String.class));
        return assignmentVO;
    }
}
