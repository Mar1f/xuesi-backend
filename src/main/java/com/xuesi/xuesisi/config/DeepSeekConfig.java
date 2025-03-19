package com.xuesi.xuesisi.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek AI服务配置
 */
@Configuration
@Getter
public class DeepSeekConfig {
    
    /**
     * API密钥
     */
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    /**
     * 使用的模型
     */
    @Value("${deepseek.model:deepseek-coder-33b-instruct}")
    private String model;
    
    /**
     * 最大生成token数
     */
    @Value("${deepseek.max-tokens:4000}")
    private Integer maxTokens;
    
    /**
     * 温度参数，控制随机性
     */
    @Value("${deepseek.temperature:0.3}")
    private Double temperature;
    
    /**
     * API URL
     */
    @Value("${deepseek.api-url}")
    private String apiUrl;
    
    /**
     * 连接超时时间(毫秒)
     */
    @Value("${deepseek.connect-timeout:5000}")
    private Integer connectTimeout;
    
    /**
     * 读取超时时间(毫秒)
     */
    @Value("${deepseek.read-timeout:30000}")
    private Integer readTimeout;
} 