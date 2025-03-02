package com.xuesi.xuesisi.model.dto.CLass;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * 班级查询响应DTO
 */
@Data
public class QueryClass {
    private Long id;
    private String className;
    private Long teacherId;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
}
