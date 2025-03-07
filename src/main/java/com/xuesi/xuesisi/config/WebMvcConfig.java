package com.xuesi.xuesisi.config;

import com.xuesi.xuesisi.interceptor.PermissionCheckInterceptor;
import com.xuesi.xuesisi.interceptor.RoleCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private RoleCheckInterceptor roleCheckInterceptor;

    @Resource
    private PermissionCheckInterceptor permissionCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册角色校验拦截器
        registry.addInterceptor(roleCheckInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(    // 不需要处理的请求路径
                        "/user/login",    // 登录接口
                        "/user/register", // 注册接口
                        "/user/logout",   // 登出接口
                        "/error"          // 错误页面
                );

        // 注册权限校验拦截器
        registry.addInterceptor(permissionCheckInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(    // 不需要处理的请求路径
                        "/user/login",    // 登录接口
                        "/user/register", // 注册接口
                        "/user/logout",   // 登出接口
                        "/error"          // 错误页面
                );
    }
} 