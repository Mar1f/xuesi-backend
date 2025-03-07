package com.xuesi.xuesisi.interceptor;

import com.xuesi.xuesisi.annotation.PermissionCheck;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.PermissionEnum;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限校验拦截器
 */
@Component
public class PermissionCheckInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果不是映射到方法，直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        PermissionCheck permissionCheck = handlerMethod.getMethodAnnotation(PermissionCheck.class);
        if (permissionCheck == null) {
            return true;
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取用户角色
        String userRole = loginUser.getUserRole();
        if (userRole == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 获取用户角色对应的权限列表
        List<String> userPermissions = Arrays.stream(PermissionEnum.getPermissionsByRole(userRole))
                .map(PermissionEnum::getCode)
                .collect(Collectors.toList());

        // 检查用户是否有所需权限
        String[] mustPermissions = permissionCheck.mustPermission();
        for (String permission : mustPermissions) {
            if (!userPermissions.contains(permission)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        return true;
    }
} 