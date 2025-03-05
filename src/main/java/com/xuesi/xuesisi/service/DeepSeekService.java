package com.xuesi.xuesisi.service;

import com.xuesi.xuesisi.config.DeepSeekConfig;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeepSeekService {

    @Resource
    private DeepSeekConfig deepSeekConfig;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 调用 DeepSeek API 进行对话
     *
     * @param prompt 对话提示词
     * @return AI 响应结果
     */
    public String chat(String prompt) {
        log.info("开始调用 DeepSeek API 进行对话");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", deepSeekConfig.getTemperature());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("发送请求到 DeepSeek API，prompt: {}", prompt);
            Map<String, Object> response = restTemplate.postForObject(deepSeekConfig.getApiUrl(), request, Map.class);
            
            if (response == null) {
                log.error("DeepSeek API 返回空响应");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回空响应");
            }
            
            if (!response.containsKey("choices")) {
                log.error("DeepSeek API 响应格式错误: {}", response);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务响应格式错误");
            }
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                log.error("DeepSeek API 未返回任何结果");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务未返回结果");
            }
            
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
            String content = (String) messageResponse.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                log.error("DeepSeek API 返回空内容");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回空内容");
            }
            
            log.info("DeepSeek API 调用成功");
            return content;
            
        } catch (RestClientException e) {
            log.error("调用 DeepSeek API 失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理 DeepSeek API 响应时发生错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务处理失败: " + e.getMessage());
        }
    }

    /**
     * 调用 DeepSeek API 进行评分
     *
     * @param prompt 评分提示词
     * @return AI 评分结果
     */
    public String getAIScore(String prompt) {
        return chat(prompt);
    }
} 