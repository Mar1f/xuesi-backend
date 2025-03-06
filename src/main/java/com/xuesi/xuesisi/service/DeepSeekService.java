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
    private static final long RETRY_DELAY = 2000; // 2 seconds

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
        requestBody.put("stream", false);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", 0.1);
        requestBody.put("top_p", 0.1);
        requestBody.put("top_k", 10);
        requestBody.put("frequency_penalty", 0.5);
        requestBody.put("presence_penalty", 0.5);
        requestBody.put("n", 1);
        
        // 添加系统消息
        List<Map<String, Object>> allMessages = new ArrayList<>();
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的题目生成器。请严格按照以下规则生成题目：\n" +
                "1. 必须返回合法的JSON格式\n" +
                "2. JSON必须包含questions数组\n" +
                "3. 每个题目必须包含以下字段：\n" +
                "   - content: 题目内容\n" +
                "   - options: 包含4个选项的数组\n" +
                "   - answer: 正确答案（A、B、C、D中的一个）\n" +
                "   - analysis: 答案解析\n" +
                "   - tags: 知识点标签数组，每个题目至少包含1个标签\n" +
                "4. options必须是包含4个选项的数组\n" +
                "5. answer必须是A、B、C、D中的一个\n" +
                "6. 不要包含任何注释或额外说明\n" +
                "7. 所有字符串必须使用英文双引号\n" +
                "8. 不要使用markdown代码块\n" +
                "9. 不要包含任何其他文本\n" +
                "10. 标签必须与题目内容相关，且符合学科知识体系");
        allMessages.add(systemMessage);
        allMessages.addAll(messages);
        requestBody.put("messages", allMessages);

        // 设置响应格式
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "text");
        requestBody.put("response_format", responseFormat);

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
            log.error("调用 DeepSeek API 失败: {}", e.getMessage());
            if (e.getMessage().contains("UnknownHostException")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法连接到 AI 服务，请检查网络连接");
            } else if (e.getMessage().contains("Connection timed out")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "连接 AI 服务超时，请稍后重试");
            } else if (e.getMessage().contains("Read timed out")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取 AI 服务响应超时，请稍后重试");
            }
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