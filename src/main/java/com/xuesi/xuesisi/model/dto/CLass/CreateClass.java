package com.xuesi.xuesisi.model.dto.CLass;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class CreateClass {
    /**
     * 班级名称
     */
    private String className;
    /**
     * 教师ID（创建班级时关联的教师）
     */
    private Integer teacherId;

    /**
     * 年级
     */
    private String grade;

    /**
     * 班级描述
     */
    private String description;
}
