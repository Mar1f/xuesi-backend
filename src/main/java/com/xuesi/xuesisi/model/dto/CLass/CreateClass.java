package com.xuesi.xuesisi.model.dto.CLass;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class CreateClass {
    private String className;
    /**
     * 教师ID（创建班级时关联的教师）
     */
    private Integer teacherId;
}
