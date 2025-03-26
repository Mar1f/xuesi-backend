package com.xuesi.xuesisi.model.dto.CLass;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class UpdateClass {
    /**
     * 班级ID
     */
    private Long id;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 年级
     */
    private String grade;

    /**
     * 班级描述
     */
    private String description;
}