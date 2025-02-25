package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.AssignmentMapper;
import com.xuesi.xuesisi.model.dto.assignment.AssignmentQueryRequest;
import com.xuesi.xuesisi.model.entity.Assignment;
import com.xuesi.xuesisi.model.entity.AssignmentFavour;
import com.xuesi.xuesisi.model.entity.AssignmentThumb;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.AssignmentVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.AssignmentService;
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
 * 题单服务实现
 *
 */
@Service
@Slf4j
public class AssignmentServiceImpl extends ServiceImpl<AssignmentMapper, Assignment> implements AssignmentService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param assignment
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validAssignment(Assignment assignment, boolean add) {
        ThrowUtils.throwIf(assignment == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = assignment.getTitle();
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
     * @param assignmentQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Assignment> getQueryWrapper(AssignmentQueryRequest assignmentQueryRequest) {
        QueryWrapper<Assignment> queryWrapper = new QueryWrapper<>();
        if (assignmentQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = assignmentQueryRequest.getId();
        Long notId = assignmentQueryRequest.getNotId();
        String title = assignmentQueryRequest.getTitle();
        String content = assignmentQueryRequest.getContent();
        String searchText = assignmentQueryRequest.getSearchText();
        String sortField = assignmentQueryRequest.getSortField();
        String sortOrder = assignmentQueryRequest.getSortOrder();
        List<String> tagList = assignmentQueryRequest.getTags();
        Long userId = assignmentQueryRequest.getUserId();
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
     * 获取题单封装
     *
     * @param assignment
     * @param request
     * @return
     */
    @Override
    public AssignmentVO getAssignmentVO(Assignment assignment, HttpServletRequest request) {
        // 对象转封装类
        AssignmentVO assignmentVO = AssignmentVO.objToVo(assignment);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = assignment.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        assignmentVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long assignmentId = assignment.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<AssignmentThumb> assignmentThumbQueryWrapper = new QueryWrapper<>();
            assignmentThumbQueryWrapper.in("assignmentId", assignmentId);
            assignmentThumbQueryWrapper.eq("userId", loginUser.getId());
            AssignmentThumb assignmentThumb = assignmentThumbMapper.selectOne(assignmentThumbQueryWrapper);
            assignmentVO.setHasThumb(assignmentThumb != null);
            // 获取收藏
            QueryWrapper<AssignmentFavour> assignmentFavourQueryWrapper = new QueryWrapper<>();
            assignmentFavourQueryWrapper.in("assignmentId", assignmentId);
            assignmentFavourQueryWrapper.eq("userId", loginUser.getId());
            AssignmentFavour assignmentFavour = assignmentFavourMapper.selectOne(assignmentFavourQueryWrapper);
            assignmentVO.setHasFavour(assignmentFavour != null);
        }
        // endregion

        return assignmentVO;
    }

    /**
     * 分页获取题单封装
     *
     * @param assignmentPage
     * @param request
     * @return
     */
    @Override
    public Page<AssignmentVO> getAssignmentVOPage(Page<Assignment> assignmentPage, HttpServletRequest request) {
        List<Assignment> assignmentList = assignmentPage.getRecords();
        Page<AssignmentVO> assignmentVOPage = new Page<>(assignmentPage.getCurrent(), assignmentPage.getSize(), assignmentPage.getTotal());
        if (CollUtil.isEmpty(assignmentList)) {
            return assignmentVOPage;
        }
        // 对象列表 => 封装对象列表
        List<AssignmentVO> assignmentVOList = assignmentList.stream().map(assignment -> {
            return AssignmentVO.objToVo(assignment);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = assignmentList.stream().map(Assignment::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> assignmentIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> assignmentIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> assignmentIdSet = assignmentList.stream().map(Assignment::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<AssignmentThumb> assignmentThumbQueryWrapper = new QueryWrapper<>();
            assignmentThumbQueryWrapper.in("assignmentId", assignmentIdSet);
            assignmentThumbQueryWrapper.eq("userId", loginUser.getId());
            List<AssignmentThumb> assignmentAssignmentThumbList = assignmentThumbMapper.selectList(assignmentThumbQueryWrapper);
            assignmentAssignmentThumbList.forEach(assignmentAssignmentThumb -> assignmentIdHasThumbMap.put(assignmentAssignmentThumb.getAssignmentId(), true));
            // 获取收藏
            QueryWrapper<AssignmentFavour> assignmentFavourQueryWrapper = new QueryWrapper<>();
            assignmentFavourQueryWrapper.in("assignmentId", assignmentIdSet);
            assignmentFavourQueryWrapper.eq("userId", loginUser.getId());
            List<AssignmentFavour> assignmentFavourList = assignmentFavourMapper.selectList(assignmentFavourQueryWrapper);
            assignmentFavourList.forEach(assignmentFavour -> assignmentIdHasFavourMap.put(assignmentFavour.getAssignmentId(), true));
        }
        // 填充信息
        assignmentVOList.forEach(assignmentVO -> {
            Long userId = assignmentVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            assignmentVO.setUser(userService.getUserVO(user));
            assignmentVO.setHasThumb(assignmentIdHasThumbMap.getOrDefault(assignmentVO.getId(), false));
            assignmentVO.setHasFavour(assignmentIdHasFavourMap.getOrDefault(assignmentVO.getId(), false));
        });
        // endregion

        assignmentVOPage.setRecords(assignmentVOList);
        return assignmentVOPage;
    }

}
