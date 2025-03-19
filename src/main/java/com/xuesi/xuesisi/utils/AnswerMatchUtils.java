package com.xuesi.xuesisi.utils;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 答案匹配工具类
 */
public class AnswerMatchUtils {

    /**
     * 计算两个字符串的相似度
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 相似度 (0-1)
     */
    public static double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        // 转换为小写并去除多余空格
        str1 = str1.toLowerCase().trim().replaceAll("\\s+", " ");
        str2 = str2.toLowerCase().trim().replaceAll("\\s+", " ");
        
        // 如果完全相同
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        // 计算编辑距离
        int distance = calculateLevenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        // 计算相似度
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * 计算编辑距离
     */
    private static int calculateLevenshteinDistance(String str1, String str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 1; j <= str2.length(); j++) {
            distance[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1)
                );
            }
        }
        
        return distance[str1.length()][str2.length()];
    }
    
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
    
    /**
     * 提取关键词
     *
     * @param text 文本
     * @return 关键词列表
     */
    public static List<String> extractKeywords(String text) {
        if (StringUtils.isBlank(text)) {
            return new ArrayList<>();
        }
        
        // 移除标点符号
        text = text.replaceAll("[\\p{P}\\p{S}]", " ");
        // 转换为小写
        text = text.toLowerCase();
        // 分词
        String[] words = text.split("\\s+");
        // 过滤停用词
        return filterStopWords(Arrays.asList(words));
    }
    
    /**
     * 过滤停用词
     */
    private static List<String> filterStopWords(List<String> words) {
        List<String> stopWords = Arrays.asList(
            "的", "了", "和", "与", "或", "在", "是", "我", "你", "他", "她", "它",
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "with"
        );
        
        List<String> filteredWords = new ArrayList<>();
        for (String word : words) {
            if (!stopWords.contains(word) && word.length() > 1) {
                filteredWords.add(word);
            }
        }
        return filteredWords;
    }
    
    /**
     * 计算填空题答案匹配度
     *
     * @param userAnswer 用户答案
     * @param correctAnswer 正确答案
     * @return 匹配度 (0-1)
     */
    public static double calculateFillBlankMatch(String userAnswer, String correctAnswer) {
        if (StringUtils.isBlank(userAnswer) || StringUtils.isBlank(correctAnswer)) {
            return 0.0;
        }
        
        // 清理答案
        userAnswer = cleanAnswer(userAnswer);
        correctAnswer = cleanAnswer(correctAnswer);
        
        // 计算相似度
        double similarity = calculateSimilarity(userAnswer, correctAnswer);
        
        // 如果相似度很高，认为是完全匹配
        if (similarity > 0.9) {
            return 1.0;
        }
        
        // 否则返回实际相似度
        return similarity;
    }
    
    /**
     * 计算简答题答案匹配度
     *
     * @param userAnswer 用户答案
     * @param referenceAnswer 参考答案
     * @param keywords 关键词列表
     * @return 匹配度 (0-1)
     */
    public static double calculateEssayMatch(String userAnswer, String referenceAnswer, List<String> keywords) {
        if (StringUtils.isBlank(userAnswer)) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // 1. 关键词匹配 (40%)
        if (keywords != null && !keywords.isEmpty()) {
            int matchCount = 0;
            List<String> userKeywords = extractKeywords(userAnswer);
            for (String keyword : keywords) {
                if (userKeywords.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            score += 0.4 * ((double) matchCount / keywords.size());
        }
        
        // 2. 内容相似度 (60%)
        if (StringUtils.isNotBlank(referenceAnswer)) {
            double similarity = calculateSimilarity(userAnswer, referenceAnswer);
            score += 0.6 * similarity;
        }
        
        return score;
    }
    
    /**
     * 清理答案文本
     */
    private static String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.toLowerCase()
                    .trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[\\p{P}\\p{S}]", "");
    }
} 