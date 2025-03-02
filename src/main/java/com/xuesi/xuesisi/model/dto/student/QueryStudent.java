package com.xuesi.xuesisi.model.dto.student;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class QueryStudent {
    private Long id;
    private String userAccount;
    private String userPassword;
    private String userName;
    private String userAvatar;
    private String userProfile;
    /**
     * 用户角色（学生固定为 "student"）
     */
    private String userRole;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
}
