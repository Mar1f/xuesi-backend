package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.SoringResultMapper;
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultQueryRequest;
import com.xuesi.xuesisi.model.entity.SoringResult;
import com.xuesi.xuesisi.model.entity.SoringResultFavour;
import com.xuesi.xuesisi.model.entity.SoringResultThumb;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.SoringResultVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.SoringResultService;
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
 * 评分结果服务实现
 *
 */
@Service
@Slf4j
public class SoringResultServiceImpl extends ServiceImpl<SoringResultMapper, SoringResult> implements SoringResultService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param soringResult
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validSoringResult(SoringResult soringResult, boolean add) {
        ThrowUtils.throwIf(soringResult == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = soringResult.getTitle();
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
     * @param soringResultQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SoringResult> getQueryWrapper(SoringResultQueryRequest soringResultQueryRequest) {
        QueryWrapper<SoringResult> queryWrapper = new QueryWrapper<>();
        if (soringResultQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = soringResultQueryRequest.getId();
        Long notId = soringResultQueryRequest.getNotId();
        String title = soringResultQueryRequest.getTitle();
        String content = soringResultQueryRequest.getContent();
        String searchText = soringResultQueryRequest.getSearchText();
        String sortField = soringResultQueryRequest.getSortField();
        String sortOrder = soringResultQueryRequest.getSortOrder();
        List<String> tagList = soringResultQueryRequest.getTags();
        Long userId = soringResultQueryRequest.getUserId();
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
     * 获取评分结果封装
     *
     * @param soringResult
     * @param request
     * @return
     */
    @Override
    public SoringResultVO getSoringResultVO(SoringResult soringResult, HttpServletRequest request) {
        // 对象转封装类
        SoringResultVO soringResultVO = SoringResultVO.objToVo(soringResult);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = soringResult.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        soringResultVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long soringResultId = soringResult.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<SoringResultThumb> soringResultThumbQueryWrapper = new QueryWrapper<>();
            soringResultThumbQueryWrapper.in("soringResultId", soringResultId);
            soringResultThumbQueryWrapper.eq("userId", loginUser.getId());
            SoringResultThumb soringResultThumb = soringResultThumbMapper.selectOne(soringResultThumbQueryWrapper);
            soringResultVO.setHasThumb(soringResultThumb != null);
            // 获取收藏
            QueryWrapper<SoringResultFavour> soringResultFavourQueryWrapper = new QueryWrapper<>();
            soringResultFavourQueryWrapper.in("soringResultId", soringResultId);
            soringResultFavourQueryWrapper.eq("userId", loginUser.getId());
            SoringResultFavour soringResultFavour = soringResultFavourMapper.selectOne(soringResultFavourQueryWrapper);
            soringResultVO.setHasFavour(soringResultFavour != null);
        }
        // endregion

        return soringResultVO;
    }

    /**
     * 分页获取评分结果封装
     *
     * @param soringResultPage
     * @param request
     * @return
     */
    @Override
    public Page<SoringResultVO> getSoringResultVOPage(Page<SoringResult> soringResultPage, HttpServletRequest request) {
        List<SoringResult> soringResultList = soringResultPage.getRecords();
        Page<SoringResultVO> soringResultVOPage = new Page<>(soringResultPage.getCurrent(), soringResultPage.getSize(), soringResultPage.getTotal());
        if (CollUtil.isEmpty(soringResultList)) {
            return soringResultVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SoringResultVO> soringResultVOList = soringResultList.stream().map(soringResult -> {
            return SoringResultVO.objToVo(soringResult);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = soringResultList.stream().map(SoringResult::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> soringResultIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> soringResultIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> soringResultIdSet = soringResultList.stream().map(SoringResult::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<SoringResultThumb> soringResultThumbQueryWrapper = new QueryWrapper<>();
            soringResultThumbQueryWrapper.in("soringResultId", soringResultIdSet);
            soringResultThumbQueryWrapper.eq("userId", loginUser.getId());
            List<SoringResultThumb> soringResultSoringResultThumbList = soringResultThumbMapper.selectList(soringResultThumbQueryWrapper);
            soringResultSoringResultThumbList.forEach(soringResultSoringResultThumb -> soringResultIdHasThumbMap.put(soringResultSoringResultThumb.getSoringResultId(), true));
            // 获取收藏
            QueryWrapper<SoringResultFavour> soringResultFavourQueryWrapper = new QueryWrapper<>();
            soringResultFavourQueryWrapper.in("soringResultId", soringResultIdSet);
            soringResultFavourQueryWrapper.eq("userId", loginUser.getId());
            List<SoringResultFavour> soringResultFavourList = soringResultFavourMapper.selectList(soringResultFavourQueryWrapper);
            soringResultFavourList.forEach(soringResultFavour -> soringResultIdHasFavourMap.put(soringResultFavour.getSoringResultId(), true));
        }
        // 填充信息
        soringResultVOList.forEach(soringResultVO -> {
            Long userId = soringResultVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            soringResultVO.setUser(userService.getUserVO(user));
            soringResultVO.setHasThumb(soringResultIdHasThumbMap.getOrDefault(soringResultVO.getId(), false));
            soringResultVO.setHasFavour(soringResultIdHasFavourMap.getOrDefault(soringResultVO.getId(), false));
        });
        // endregion

        soringResultVOPage.setRecords(soringResultVOList);
        return soringResultVOPage;
    }

}
