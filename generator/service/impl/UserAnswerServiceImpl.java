package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.entity.UserAnswerFavour;
import com.xuesi.xuesisi.model.entity.UserAnswerThumb;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.UserService;
import com.xuesi.xuesisi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学生答案服务实现
 *
 */
@Service
@Slf4j
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param userAnswer
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validUserAnswer(UserAnswer userAnswer, boolean add) {
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = userAnswer.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
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
        // todo 从对象中取值
        Long id = userAnswerQueryRequest.getId();
        Long notId = userAnswerQueryRequest.getNotId();
        String title = userAnswerQueryRequest.getTitle();
        String content = userAnswerQueryRequest.getContent();
        String searchText = userAnswerQueryRequest.getSearchText();
        String sortField = userAnswerQueryRequest.getSortField();
        String sortOrder = userAnswerQueryRequest.getSortOrder();
        List<String> tagList = userAnswerQueryRequest.getTags();
        Long userId = userAnswerQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取学生答案封装
     *
     * @param userAnswer
     * @param request
     * @return
     */
    @Override
    public UserAnswerVO getUserAnswerVO(UserAnswer userAnswer, HttpServletRequest request) {
        // 对象转封装类
        UserAnswerVO userAnswerVO = UserAnswerVO.objToVo(userAnswer);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = userAnswer.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        userAnswerVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long userAnswerId = userAnswer.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<UserAnswerThumb> userAnswerThumbQueryWrapper = new QueryWrapper<>();
            userAnswerThumbQueryWrapper.in("userAnswerId", userAnswerId);
            userAnswerThumbQueryWrapper.eq("userId", loginUser.getId());
            UserAnswerThumb userAnswerThumb = userAnswerThumbMapper.selectOne(userAnswerThumbQueryWrapper);
            userAnswerVO.setHasThumb(userAnswerThumb != null);
            // 获取收藏
            QueryWrapper<UserAnswerFavour> userAnswerFavourQueryWrapper = new QueryWrapper<>();
            userAnswerFavourQueryWrapper.in("userAnswerId", userAnswerId);
            userAnswerFavourQueryWrapper.eq("userId", loginUser.getId());
            UserAnswerFavour userAnswerFavour = userAnswerFavourMapper.selectOne(userAnswerFavourQueryWrapper);
            userAnswerVO.setHasFavour(userAnswerFavour != null);
        }
        // endregion

        return userAnswerVO;
    }

    /**
     * 分页获取学生答案封装
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

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = userAnswerList.stream().map(UserAnswer::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> userAnswerIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> userAnswerIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> userAnswerIdSet = userAnswerList.stream().map(UserAnswer::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<UserAnswerThumb> userAnswerThumbQueryWrapper = new QueryWrapper<>();
            userAnswerThumbQueryWrapper.in("userAnswerId", userAnswerIdSet);
            userAnswerThumbQueryWrapper.eq("userId", loginUser.getId());
            List<UserAnswerThumb> userAnswerUserAnswerThumbList = userAnswerThumbMapper.selectList(userAnswerThumbQueryWrapper);
            userAnswerUserAnswerThumbList.forEach(userAnswerUserAnswerThumb -> userAnswerIdHasThumbMap.put(userAnswerUserAnswerThumb.getUserAnswerId(), true));
            // 获取收藏
            QueryWrapper<UserAnswerFavour> userAnswerFavourQueryWrapper = new QueryWrapper<>();
            userAnswerFavourQueryWrapper.in("userAnswerId", userAnswerIdSet);
            userAnswerFavourQueryWrapper.eq("userId", loginUser.getId());
            List<UserAnswerFavour> userAnswerFavourList = userAnswerFavourMapper.selectList(userAnswerFavourQueryWrapper);
            userAnswerFavourList.forEach(userAnswerFavour -> userAnswerIdHasFavourMap.put(userAnswerFavour.getUserAnswerId(), true));
        }
        // 填充信息
        userAnswerVOList.forEach(userAnswerVO -> {
            Long userId = userAnswerVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            userAnswerVO.setUser(userService.getUserVO(user));
            userAnswerVO.setHasThumb(userAnswerIdHasThumbMap.getOrDefault(userAnswerVO.getId(), false));
            userAnswerVO.setHasFavour(userAnswerIdHasFavourMap.getOrDefault(userAnswerVO.getId(), false));
        });
        // endregion

        userAnswerVOPage.setRecords(userAnswerVOList);
        return userAnswerVOPage;
    }

}
