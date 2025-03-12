package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.ScoringResultMapper;
import com.xuesi.xuesisi.model.dto.scoringResult.ScoringResultQueryRequest;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.ScoringResult;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.ScoringResultVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.ScoringResultService;
import com.xuesi.xuesisi.service.UserService;
import com.xuesi.xuesisi.service.DeepSeekService;
import com.xuesi.xuesisi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
* @author mar1
* @description 针对表【scoring_result(评分结果表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/


/**
 * 评分结果服务实现
 *
 */
@Service
@Slf4j
public class ScoringResultServiceImpl extends ServiceImpl<ScoringResultMapper, ScoringResult> implements ScoringResultService {

    @Resource
    private UserService userService;

    @Resource
    private DeepSeekService deepSeekService;

    /**
     * 校验数据
     *
     * @param scoringResult
     * @param add           对创建的数据进行校验
     */
    @Override
    public void validScoringResult(ScoringResult scoringResult, boolean add) {
        ThrowUtils.throwIf(scoringResult == null, ErrorCode.PARAMS_ERROR);
        
        // 从对象中取值
        String resultName = scoringResult.getResultName();
        Long questionBankId = scoringResult.getQuestionBankId();
        
        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(resultName), ErrorCode.PARAMS_ERROR, "结果名称不能为空");
            ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "questionBankId 非法");
        }
        
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (StringUtils.isNotBlank(resultName)) {
            ThrowUtils.throwIf(resultName.length() > 128, ErrorCode.PARAMS_ERROR, "结果名称不能超过 128");
        }
    }

    /**
     * 获取查询条件
     *
     * @param scoringResultQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<ScoringResult> getQueryWrapper(ScoringResultQueryRequest scoringResultQueryRequest) {
        QueryWrapper<ScoringResult> queryWrapper = new QueryWrapper<>();
        if (scoringResultQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = scoringResultQueryRequest.getId();
        String resultName = scoringResultQueryRequest.getResultName();
        String resultDesc = scoringResultQueryRequest.getResultDesc();
//        String resultPicture = scoringResultQueryRequest.getResultPicture();
//        String resultProp = scoringResultQueryRequest.getResultProp();
        Integer resultScoreRange = scoringResultQueryRequest.getResultScoreRange();
        Long questionBankId = scoringResultQueryRequest.getQuestionBankId();
        Long userId = scoringResultQueryRequest.getUserId();
        Long notId = scoringResultQueryRequest.getNotId();
        String searchText = scoringResultQueryRequest.getSearchText();
        String sortField = scoringResultQueryRequest.getSortField();
        String sortOrder = scoringResultQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("resultName", searchText).or().like("resultDesc", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(resultName), "resultName", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "resultDesc", resultDesc);
//        queryWrapper.like(StringUtils.isNotBlank(resultProp), "resultProp", resultProp);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultScoreRange), "resultScoreRange", resultScoreRange);
//        queryWrapper.eq(StringUtils.isNotBlank(resultPicture), "resultPicture", resultPicture);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取评分结果封装
     *
     * @param scoringResult
     * @param request
     * @return
     */
    @Override
    public ScoringResultVO getScoringResultVO(ScoringResult scoringResult, HttpServletRequest request) {
        // 对象转封装类
        ScoringResultVO scoringResultVO = ScoringResultVO.objToVo(scoringResult);

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = scoringResult.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        scoringResultVO.setUser(userVO);
        // endregion

        return scoringResultVO;
    }

    /**
     * 分页获取评分结果封装
     *
     * @param scoringResultPage
     * @param request
     * @return
     */
    @Override
    public Page<ScoringResultVO> getScoringResultVOPage(Page<ScoringResult> scoringResultPage, HttpServletRequest request) {
        List<ScoringResult> scoringResultList = scoringResultPage.getRecords();
        Page<ScoringResultVO> scoringResultVOPage = new Page<>(scoringResultPage.getCurrent(), scoringResultPage.getSize(), scoringResultPage.getTotal());
        if (CollUtil.isEmpty(scoringResultList)) {
            return scoringResultVOPage;
        }
        // 对象列表 => 封装对象列表
        List<ScoringResultVO> scoringResultVOList = scoringResultList.stream().map(scoringResult -> {
            return ScoringResultVO.objToVo(scoringResult);
        }).collect(Collectors.toList());

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = scoringResultList.stream().map(ScoringResult::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        scoringResultVOList.forEach(scoringResultVO -> {
            Long userId = scoringResultVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            scoringResultVO.setUser(userService.getUserVO(user));
        });
        // endregion

        scoringResultVOPage.setRecords(scoringResultVOList);
        return scoringResultVOPage;
    }

    @Override
    public void createDefaultScoringResults(QuestionBank questionBank) {
        // 检查是否已经有评分结果
        List<ScoringResult> existingResults = list(
            new QueryWrapper<ScoringResult>()
                .eq("questionBankId", questionBank.getId())
        );
        
        // 如果已经有评分结果，则不重复创建
        if (!existingResults.isEmpty()) {
            return;
        }

        try {
            // 构建AI提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下题目内容，生成5个评分等级及其描述。\n\n");
            prompt.append("要求：\n");
            prompt.append("1. 必须严格按照指定的JSON格式返回\n");
            prompt.append("2. 所有字符串必须使用双引号包裹\n");
            prompt.append("3. 分数范围必须是数字（如：90、80、70、60、0）\n");
            prompt.append("4. 不要包含任何注释或额外说明\n\n");
            prompt.append("题目内容：\n");
            prompt.append(questionBank.getDescription());
            prompt.append("\n\n返回格式示例：\n");
            prompt.append("{\n");
            prompt.append("  \"results\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"name\": \"优秀\",\n");
            prompt.append("      \"scoreRange\": 90,\n");
            prompt.append("      \"description\": \"表现优秀，继续保持！\"\n");
            prompt.append("    },\n");
            prompt.append("    {\n");
            prompt.append("      \"name\": \"良好\",\n");
            prompt.append("      \"scoreRange\": 80,\n");
            prompt.append("      \"description\": \"表现不错，仍有提升空间。\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}\n\n");
            prompt.append("请严格按照上述格式返回，确保JSON格式正确且完整。");

            // 调用DeepSeek生成评分结果
            String aiResponse = deepSeekService.chat(prompt.toString());
            log.info("AI生成的评分结果响应: {}", aiResponse);
            
            // 尝试解析AI响应
            JSONObject jsonResponse;
            try {
                // 预处理JSON字符串
                String jsonStr = preprocessJson(aiResponse);
                
                // 尝试解析JSON
                try {
                    jsonResponse = JSONUtil.parseObj(jsonStr);
                } catch (Exception e) {
                    log.error("JSON解析失败，尝试修复格式: {}", e.getMessage());
                    // 如果解析失败，尝试进一步清理和修复JSON格式
                    jsonStr = jsonStr.replaceAll("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", "") // 移除引号外的空白
                                   .replaceAll("(?<![\"{\\[,])\\s+(?![\"\\]}])", ""); // 保留必要的空格
                    log.info("修复后的JSON字符串: {}", jsonStr);
                    jsonResponse = JSONUtil.parseObj(jsonStr);
                }
            } catch (Exception e) {
                log.error("解析AI响应JSON失败: {}", e.getMessage());
                log.error("原始响应内容: {}", aiResponse);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应格式错误，请重试");
            }
            
            if (!jsonResponse.containsKey("results")) {
                log.error("AI响应缺少results字段: {}", jsonResponse);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应格式错误，缺少必要字段");
            }
            
            JSONArray results = jsonResponse.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                log.error("AI响应results为空: {}", jsonResponse);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI响应格式错误，结果为空");
            }
            
            // 保存评分结果
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                if (!result.containsKey("name") || !result.containsKey("scoreRange") || !result.containsKey("description")) {
                    log.error("评分结果缺少必要字段: {}", result);
                    continue;
                }
                
                ScoringResult scoringResult = new ScoringResult();
                scoringResult.setResultName(result.getStr("name"));
                scoringResult.setResultDesc(result.getStr("description"));
                // 处理分数范围，确保是数字
                String scoreRangeStr = result.getStr("scoreRange");
                int scoreRange;
                try {
                    // 如果是字符串，尝试提取数字
                    if (scoreRangeStr.contains("分")) {
                        scoreRangeStr = scoreRangeStr.replaceAll("[^0-9]", "");
                    }
                    scoreRange = Integer.parseInt(scoreRangeStr);
                } catch (NumberFormatException e) {
                    log.error("分数范围格式错误: {}", scoreRangeStr);
                    continue;
                }
                scoringResult.setResultScoreRange(scoreRange);
                scoringResult.setIsDynamic(1); // 标记为AI生成
                scoringResult.setQuestionBankId(questionBank.getId());
                scoringResult.setUserId(questionBank.getUserId());
                save(scoringResult);
                log.info("保存评分结果: {}", scoringResult);
            }
        } catch (Exception e) {
            log.error("AI生成评分结果失败", e);
            // 如果AI生成失败，使用默认的评分结果
            createDefaultScoringResultsFallback(questionBank);
        }
    }

    /**
     * 创建默认评分结果（备用方案）
     */
    private void createDefaultScoringResultsFallback(QuestionBank questionBank) {
        // 优秀
        ScoringResult excellent = new ScoringResult();
        excellent.setResultName("优秀");
        excellent.setResultDesc("表现优秀，继续保持！");
        excellent.setResultScoreRange(90);
        excellent.setIsDynamic(0);
        excellent.setQuestionBankId(questionBank.getId());
        excellent.setUserId(questionBank.getUserId());
        save(excellent);

        // 良好
        ScoringResult good = new ScoringResult();
        good.setResultName("良好");
        good.setResultDesc("表现不错，仍有提升空间。");
        good.setResultScoreRange(80);
        good.setIsDynamic(0);
        good.setQuestionBankId(questionBank.getId());
        good.setUserId(questionBank.getUserId());
        save(good);

        // 中等
        ScoringResult medium = new ScoringResult();
        medium.setResultName("中等");
        medium.setResultDesc("表现一般，有较大的提升空间。");
        medium.setResultScoreRange(70);
        medium.setIsDynamic(0);
        medium.setQuestionBankId(questionBank.getId());
        medium.setUserId(questionBank.getUserId());
        save(medium);

        // 及格
        ScoringResult pass = new ScoringResult();
        pass.setResultName("及格");
        pass.setResultDesc("基本掌握，需要加强练习。");
        pass.setResultScoreRange(60);
        pass.setIsDynamic(0);
        pass.setQuestionBankId(questionBank.getId());
        pass.setUserId(questionBank.getUserId());
        save(pass);

        // 不及格
        ScoringResult fail = new ScoringResult();
        fail.setResultName("不及格");
        fail.setResultDesc("需要更多努力，建议重新学习。");
        fail.setResultScoreRange(0);
        fail.setIsDynamic(0);
        fail.setQuestionBankId(questionBank.getId());
        fail.setUserId(questionBank.getUserId());
        save(fail);
    }

    /**
     * 预处理JSON字符串
     */
    private String preprocessJson(String jsonStr) {
        // 移除 Markdown 代码块标记
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("```"));
            }
        }
        jsonStr = jsonStr.trim();
        
        // 预处理 JSON 字符串
        jsonStr = jsonStr.replaceAll("\\\\n\\s*", "") // 移除转义的换行符及其后的空白
                       .replaceAll("\\s*\\\\\"\\s*", "\"") // 处理转义的引号
                       .replaceAll("\\\\([^\"\\\\])", "$1") // 移除不必要的反斜杠
                       .replaceAll("\"\\s*:\\s*\"", "\": \"") // 规范化键值对格式
                       .replaceAll(",\\s*}", "}") // 处理对象末尾多余的逗号
                       .replaceAll(",\\s*]", "]") // 处理数组末尾多余的逗号
                       .replaceAll("\"\\s*\"", "\", \"") // 处理缺少逗号的情况
                       .replaceAll("\\}\\s*\\{", "}, {"); // 处理对象之间缺少逗号的情况
        
        // 处理特殊字符和转义序列
        jsonStr = jsonStr.replace("\\\"", "\"")
                       .replace("\\n", "")
                       .replace("\\r", "")
                       .replace("\\t", "");
        
        log.info("预处理后的JSON字符串: {}", jsonStr);
        
        return jsonStr;
    }

}




