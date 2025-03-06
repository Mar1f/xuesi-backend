package com.xuesi.xuesisi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {
    
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    @Value("${deepseek.model:deepseek-coder-33b-instruct}")
    private String model;
    
    @Value("${deepseek.max-tokens:4000}")
    private Integer maxTokens;
    
    @Value("${deepseek.temperature:0.3}")
    private Double temperature;
    
    @Value("${deepseek.api-url}")
    private String apiUrl;
    
    @Value("${deepseek.connect-timeout:5000}")
    private Integer connectTimeout;
    
    @Value("${deepseek.read-timeout:30000}")
    private Integer readTimeout;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public Integer getConnectTimeout() {
        return connectTimeout;
    }
    
    public Integer getReadTimeout() {
        return readTimeout;
    }
} 