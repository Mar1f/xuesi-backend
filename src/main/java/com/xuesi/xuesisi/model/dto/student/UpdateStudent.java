package com.xuesi.xuesisi.model.dto.student;

import lombok.Data;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@Data
public class UpdateStudent {
    private String userAccount;
    private String userPassword;
    private String userName;
    private String userAvatar;
    private String userProfile;
}
