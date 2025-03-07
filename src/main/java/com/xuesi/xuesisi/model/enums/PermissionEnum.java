package com.xuesi.xuesisi.model.enums;

import com.xuesi.xuesisi.constant.RoleConstant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限枚举
 */
public enum PermissionEnum {
    // 班级管理权限
    CLASS_CREATE("class:create", "创建班级"),
    CLASS_UPDATE("class:update", "更新班级"),
    CLASS_DELETE("class:delete", "删除班级"),
    CLASS_VIEW("class:view", "查看班级"),
    CLASS_MANAGE("class:manage", "管理班级"),

    // 题库管理权限
    QUESTION_BANK_CREATE("questionBank:create", "创建题库"),
    QUESTION_BANK_UPDATE("questionBank:update", "更新题库"),
    QUESTION_BANK_DELETE("questionBank:delete", "删除题库"),
    QUESTION_BANK_VIEW("questionBank:view", "查看题库"),
    QUESTION_BANK_MANAGE("questionBank:manage", "管理题库"),

    // 题目管理权限
    QUESTION_CREATE("question:create", "创建题目"),
    QUESTION_UPDATE("question:update", "更新题目"),
    QUESTION_DELETE("question:delete", "删除题目"),
    QUESTION_VIEW("question:view", "查看题目"),
    QUESTION_MANAGE("question:manage", "管理题目"),

    // 用户管理权限
    USER_CREATE("user:create", "创建用户"),
    USER_UPDATE("user:update", "更新用户"),
    USER_DELETE("user:delete", "删除用户"),
    USER_VIEW("user:view", "查看用户"),
    USER_MANAGE("user:manage", "管理用户");

    private final String code;
    private final String description;

    PermissionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据角色获取对应的权限列表
     */
    public static PermissionEnum[] getPermissionsByRole(String role) {
        switch (role) {
            case RoleConstant.ADMIN_ROLE:
                return values(); // 管理员拥有所有权限
            case RoleConstant.TEACHER_ROLE:
                return new PermissionEnum[]{
                    CLASS_CREATE, CLASS_UPDATE, CLASS_VIEW, CLASS_MANAGE,
                    QUESTION_BANK_CREATE, QUESTION_BANK_UPDATE, QUESTION_BANK_VIEW, QUESTION_BANK_MANAGE,
                    QUESTION_CREATE, QUESTION_UPDATE, QUESTION_VIEW, QUESTION_MANAGE,
                    USER_VIEW
                };
            case RoleConstant.STUDENT_ROLE:
                return new PermissionEnum[]{
                    CLASS_VIEW,
                    QUESTION_BANK_VIEW,
                    QUESTION_VIEW
                };
            default:
                return new PermissionEnum[]{};
        }
    }

    /**
     * 根据权限码获取权限枚举
     */
    public static PermissionEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(permission -> permission.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
} 