package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.UserAnswerMapper;
import com.xuesi.xuesisi.model.dto.userAnswer.UserAnswerQueryRequest;
import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.entity.UserAnswer;
import com.xuesi.xuesisi.model.vo.UserAnswerVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.QuestionBankService;
import com.xuesi.xuesisi.service.UserAnswerService;
import com.xuesi.xuesisi.service.UserService;
import com.xuesi.xuesisi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author mar1
* @description 针对表【user_answer(用户答题记录表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/

/**
 * 用户答案服务实现
 *
 */
@Service
@Slf4j
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankService questionBankService;

    /**
     * 校验数据
     *
     * @param userAnswer
     * @param add        对创建的数据进行校验
     */
    @Override
    public void validUserAnswer(UserAnswer userAnswer, boolean add) {
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long questionBankId = userAnswer.getQuestionBankId();
        String choices = userAnswer.getChoices();
        log.info("验证前的 questionBankId: {}", questionBankId);

        // 创建数据时，参数不能为空
        log.info("Received questionBankId: {}", userAnswer.getQuestionBankId());
        if (add) {
            log.info("Received questionBankId: {}", userAnswer.getQuestionBankId());
            // 补充校验规则
            ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "questionBankId 非法");
            ThrowUtils.throwIf(StringUtils.isBlank(choices), ErrorCode.PARAMS_ERROR, "答案列表不能为空");
        }
        // 修改数据时，有参数则校验
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }


    /**
     * 获取查询条件
     *
     * @param userAnswerQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<UserAnswer> getQueryWrapper(UserAnswerQueryRequest userAnswerQueryRequest) {
        QueryWrapper<UserAnswer> queryWrapper = new QueryWrapper<>();
        if (userAnswerQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = userAnswerQueryRequest.getId();
        Long questionBankId = userAnswerQueryRequest.getQuestionBankId();
        Integer QuestionBankType = userAnswerQueryRequest.getQuestionBankType();
        Integer scoringStrategy = userAnswerQueryRequest.getScoringStrategy();
        String choices = userAnswerQueryRequest.getChoices();
        Long resultId = userAnswerQueryRequest.getResultId();
//        String resultName = userAnswerQueryRequest.getResultName();
        String resultDesc = userAnswerQueryRequest.getResultDesc();
//        String resultPicture = userAnswerQueryRequest.getResultPicture();
        Integer resultScore = userAnswerQueryRequest.getResultScore();
        Long userId = userAnswerQueryRequest.getUserId();
        Long notId = userAnswerQueryRequest.getNotId();
        String searchText = userAnswerQueryRequest.getSearchText();
        String sortField = userAnswerQueryRequest.getSortField();
        String sortOrder = userAnswerQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("resultName", searchText).or().like("resultDesc", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(choices), "choices", choices);
//        queryWrapper.like(StringUtils.isNotBlank(resultName), "resultName", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "resultDesc", resultDesc);
//        queryWrapper.like(StringUtils.isNotBlank(resultPicture), "resultPicture", resultPicture);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultId), "resultId", resultId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(QuestionBankType), "QuestionBankType", QuestionBankType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultScore), "resultScore", resultScore);
        queryWrapper.eq(ObjectUtils.isNotEmpty(scoringStrategy), "scoringStrategy", scoringStrategy);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取用户答案封装
     *
     * @param userAnswer
     * @param request
     * @return
     */
    @Override
    public UserAnswerVO getUserAnswerVO(UserAnswer userAnswer, HttpServletRequest request) {
        // 对象转封装类
        UserAnswerVO userAnswerVO = UserAnswerVO.objToVo(userAnswer);

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = userAnswer.getUserAnswerId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        userAnswerVO.setUser(userVO);
        // endregion

        return userAnswerVO;
    }

    /**
     * 分页获取用户答案封装
     *
     * @param userAnswerPage
     * @param request
     * @return
     */
    @Override
    public Page<UserAnswerVO> getUserAnswerVOPage(Page<UserAnswer> userAnswerPage, HttpServletRequest request) {
        List<UserAnswer> userAnswerList = userAnswerPage.getRecords();
        Page<UserAnswerVO> userAnswerVOPage = new Page<>(userAnswerPage.getCurrent(), userAnswerPage.getSize(), userAnswerPage.getTotal());
        if (CollUtil.isEmpty(userAnswerList)) {
            return userAnswerVOPage;
        }
        // 对象列表 => 封装对象列表
        List<UserAnswerVO> userAnswerVOList = userAnswerList.stream().map(userAnswer -> {
            return UserAnswerVO.objToVo(userAnswer);
        }).collect(Collectors.toList());

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = userAnswerList.stream().map(UserAnswer::getUserAnswerId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        userAnswerVOList.forEach(userAnswerVO -> {
            Long userId = userAnswerVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            userAnswerVO.setUser(userService.getUserVO(user));
        });
        // endregion

        userAnswerVOPage.setRecords(userAnswerVOList);
        return userAnswerVOPage;
    }

}




