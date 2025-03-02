package com.xuesi.xuesisi.model.dto.student;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class CreateStudent {
    private String userAccount;
    private String userPassword;
    private String userName;
    private String userAvatar;
    private String userProfile;
    // 学生角色固定为 "student"，可在 Service 中强制设置
}
