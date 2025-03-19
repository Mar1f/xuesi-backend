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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import cn.hutool.json.JSONUtil;

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

    private static final int MAX_RETRIES = 3;
    private static final int MAX_TOKENS = 2048;
    private static final long RETRY_DELAY = 2000; // 2 seconds
    private static final float TEMPERATURE = 0.05f; // 降低温度值，让生成更加精确

    /**
     * DeepSeek API 默认系统消息
     */
    private static final String DEFAULT_SYSTEM_MESSAGE = 
        "你是一个专业的教育辅助AI，现在需要你生成高质量的学习题目。\n\n" +
        "数学公式格式要求：\n" +
        "- 使用纯文本表示数学公式，不要使用LaTeX格式\n" +
        "- 使用^表示指数，如x^2表示x的平方\n" +
        "- 使用*表示乘法，如2*x表示2乘以x\n" +
        "- 使用/表示除法，如a/b表示a除以b\n" +
        "- 分数表示为a/b的形式\n" +
        "- 使用sqrt()表示平方根，如sqrt(x)表示x的平方根\n" +
        "- 使用sin(), cos(), tan()等表示三角函数\n" +
        "- 使用pi表示π，e表示自然对数的底数\n" +
        "- 使用|x|表示绝对值\n\n" +
        
        "JSON格式要求：\n" +
        "1. 必须生成有效的JSON格式，确保：\n" +
        "   - 所有字符串使用双引号(\")包围\n" +
        "   - 不能有未闭合的引号、括号或大括号\n" +
        "   - 数组和对象的最后一个元素后不能有逗号\n" +
        "   - 不能有注释\n" +
        "2. 所有字段都必须严格按照示例格式提供\n" +
        "3. 不要在JSON中使用\\指令或LaTeX命令\n" +
        "4. 生成的JSON必须可以被标准JSON解析器解析\n\n" +
        
        "题目内容要求：\n" +
        "1. 生成的题目应该符合高中数学或语文的教学大纲\n" +
        "2. 题目难度适中，既有基础题，也有一定挑战性的题目\n" +
        "3. 内容准确无误，尤其是数学题的解答和答案\n" +
        "4. 每个题目必须包含知识点标签(knowledgeTags)，用于分类和分析\n" +
        "5. 必须正确设置score字段为10分\n\n" +
        
        "请根据用户的具体要求，生成对应题型的题目，确保JSON格式无误并严格遵循纯文本格式的数学表达式规范。";

    /**
     * 调用 DeepSeek API 进行对话
     *
     * @param prompt 对话提示词
     * @return AI 响应结果
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = RETRY_DELAY)
    )
    public String chat(String prompt) {
        log.info("调用 DeepSeek API，提示词长度: {}", prompt.length());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());
        
        Map<String, Object> requestBody = buildRequestBody(prompt);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = restTemplate.postForObject(
                deepSeekConfig.getApiUrl(), 
                request, 
                Map.class
            );
            long endTime = System.currentTimeMillis();
            log.info("DeepSeek API 请求耗时: {}ms", (endTime - startTime));

            return extractResponseContent(response);
        } catch (RestClientException e) {
            log.error("调用 DeepSeek API 失败: {}", e.getMessage());
            handleRestClientException(e);
            return null; // 永远不会执行到这里，因为handleRestClientException会抛出异常
        } catch (Exception e) {
            log.error("处理 DeepSeek API 响应时发生错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务处理失败: " + e.getMessage());
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        // 创建用户消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        // 创建系统消息
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", DEFAULT_SYSTEM_MESSAGE);
        
        // 组合所有消息
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("temperature", TEMPERATURE);
        
        // 设置响应格式
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "text");
        requestBody.put("response_format", responseFormat);
        
        return requestBody;
    }

    /**
     * 从响应中提取内容
     */
    private String extractResponseContent(Map<String, Object> response) {
        if (response == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回空响应");
        }
        
        if (!response.containsKey("choices")) {
            handleApiErrorResponse(response);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务未返回结果");
        }

        Map<String, Object> choice = choices.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
        String content = (String) messageResponse.get("content");

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回空内容");
        }

        return content;
    }

    /**
     * 处理API错误响应
     */
    private void handleApiErrorResponse(Map<String, Object> response) {
        String errorMessage = "AI 服务响应格式错误";
        
        if (response.containsKey("error")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            errorMessage = "AI 服务错误: " + error.getOrDefault("message", "未知错误");
            log.error("DeepSeek API 错误: type={}, message={}", 
                    error.getOrDefault("type", "unknown"), 
                    error.getOrDefault("message", "unknown"));
        }
        
        throw new BusinessException(ErrorCode.OPERATION_ERROR, errorMessage);
    }

    /**
     * 处理RestClientException
     */
    private void handleRestClientException(RestClientException e) {
        String errorMessage = "AI 服务调用失败";
        
        if (e.getMessage().contains("UnknownHostException")) {
            errorMessage = "无法连接到 AI 服务，请检查网络连接";
        } else if (e.getMessage().contains("Connection timed out")) {
            errorMessage = "连接 AI 服务超时，请稍后重试";
        } else if (e.getMessage().contains("Read timed out")) {
            errorMessage = "读取 AI 服务响应超时，请稍后重试";
        } else {
            errorMessage += ": " + e.getMessage();
        }
        
        throw new BusinessException(ErrorCode.OPERATION_ERROR, errorMessage);
    }
} 