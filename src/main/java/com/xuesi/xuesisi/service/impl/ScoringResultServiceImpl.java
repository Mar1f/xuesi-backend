package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
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
            prompt.append("请根据以下题目内容，生成4个评分等级及其描述。每个等级需要包含：\n");
            prompt.append("1. 等级名称（如：优秀、良好、及格、不及格）\n");
            prompt.append("2. 分数范围（如：90分以上、80-89分、60-79分、60分以下）\n");
            prompt.append("3. 详细描述（包含对该等级表现的详细说明）\n\n");
            prompt.append("题目内容：\n");
            prompt.append(questionBank.getDescription());
            prompt.append("\n\n请以JSON格式返回，格式如下：\n");
            prompt.append("{\n");
            prompt.append("  \"results\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"name\": \"等级名称\",\n");
            prompt.append("      \"scoreRange\": 分数范围,\n");
            prompt.append("      \"description\": \"详细描述\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}");

            // 调用DeepSeek生成评分结果
            String aiResponse = deepSeekService.chat(prompt.toString());
            
            // 解析AI响应
            JSONObject jsonResponse = JSONUtil.parseObj(aiResponse);
            JSONArray results = jsonResponse.getJSONArray("results");
            
            // 保存评分结果
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                ScoringResult scoringResult = new ScoringResult();
                scoringResult.setResultName(result.getStr("name"));
                scoringResult.setResultDesc(result.getStr("description"));
                scoringResult.setResultScoreRange(result.getInt("scoreRange"));
                scoringResult.setIsDynamic(1); // 标记为AI生成
                scoringResult.setQuestionBankId(questionBank.getId());
                scoringResult.setUserId(questionBank.getUserId());
                save(scoringResult);
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

}




