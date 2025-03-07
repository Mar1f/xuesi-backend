package com.xuesi.xuesisi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 班级视图对象
 */
@Data
public class ClassVO implements Serializable {
    /**
     * 班级ID
     */
    private Long id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 班级描述
     */
    private String description;

    /**
     * 班主任ID
     */
    private Long headTeacherId;

    /**
     * 班主任姓名
     */
    private String headTeacherName;

    /**
     * 学生ID列表
     */
    private List<Long> studentIds;

    /**
     * 学生列表
     */
    private List<UserVO> students;

    /**
     * 教师ID列表
     */
    private List<Long> teacherIds;

    /**
     * 教师列表
     */
    private List<UserVO> teachers;

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
} 