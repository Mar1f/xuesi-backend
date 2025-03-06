package com.xuesi.xuesisi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RestTemplateConfig {

    @Value("${deepseek.connect-timeout:10000}")
    private Integer connectTimeout;

    @Value("${deepseek.read-timeout:60000}")
    private Integer readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout); // 连接超时时间
        factory.setReadTimeout(readTimeout); // 读取超时时间
        return new RestTemplate(factory);
    }
} 