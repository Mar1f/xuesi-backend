package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.AnalysisMapper;
import com.xuesi.xuesisi.model.dto.analysis.AnalysisQueryRequest;
import com.xuesi.xuesisi.model.entity.Analysis;
import com.xuesi.xuesisi.model.entity.AnalysisFavour;
import com.xuesi.xuesisi.model.entity.AnalysisThumb;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.AnalysisVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.AnalysisService;
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
 * 学习分析服务实现
 *
 */
@Service
@Slf4j
public class AnalysisServiceImpl extends ServiceImpl<AnalysisMapper, Analysis> implements AnalysisService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param analysis
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validAnalysis(Analysis analysis, boolean add) {
        ThrowUtils.throwIf(analysis == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = analysis.getTitle();
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
     * @param analysisQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Analysis> getQueryWrapper(AnalysisQueryRequest analysisQueryRequest) {
        QueryWrapper<Analysis> queryWrapper = new QueryWrapper<>();
        if (analysisQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = analysisQueryRequest.getId();
        Long notId = analysisQueryRequest.getNotId();
        String title = analysisQueryRequest.getTitle();
        String content = analysisQueryRequest.getContent();
        String searchText = analysisQueryRequest.getSearchText();
        String sortField = analysisQueryRequest.getSortField();
        String sortOrder = analysisQueryRequest.getSortOrder();
        List<String> tagList = analysisQueryRequest.getTags();
        Long userId = analysisQueryRequest.getUserId();
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
     * 获取学习分析封装
     *
     * @param analysis
     * @param request
     * @return
     */
    @Override
    public AnalysisVO getAnalysisVO(Analysis analysis, HttpServletRequest request) {
        // 对象转封装类
        AnalysisVO analysisVO = AnalysisVO.objToVo(analysis);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = analysis.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        analysisVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long analysisId = analysis.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<AnalysisThumb> analysisThumbQueryWrapper = new QueryWrapper<>();
            analysisThumbQueryWrapper.in("analysisId", analysisId);
            analysisThumbQueryWrapper.eq("userId", loginUser.getId());
            AnalysisThumb analysisThumb = analysisThumbMapper.selectOne(analysisThumbQueryWrapper);
            analysisVO.setHasThumb(analysisThumb != null);
            // 获取收藏
            QueryWrapper<AnalysisFavour> analysisFavourQueryWrapper = new QueryWrapper<>();
            analysisFavourQueryWrapper.in("analysisId", analysisId);
            analysisFavourQueryWrapper.eq("userId", loginUser.getId());
            AnalysisFavour analysisFavour = analysisFavourMapper.selectOne(analysisFavourQueryWrapper);
            analysisVO.setHasFavour(analysisFavour != null);
        }
        // endregion

        return analysisVO;
    }

    /**
     * 分页获取学习分析封装
     *
     * @param analysisPage
     * @param request
     * @return
     */
    @Override
    public Page<AnalysisVO> getAnalysisVOPage(Page<Analysis> analysisPage, HttpServletRequest request) {
        List<Analysis> analysisList = analysisPage.getRecords();
        Page<AnalysisVO> analysisVOPage = new Page<>(analysisPage.getCurrent(), analysisPage.getSize(), analysisPage.getTotal());
        if (CollUtil.isEmpty(analysisList)) {
            return analysisVOPage;
        }
        // 对象列表 => 封装对象列表
        List<AnalysisVO> analysisVOList = analysisList.stream().map(analysis -> {
            return AnalysisVO.objToVo(analysis);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = analysisList.stream().map(Analysis::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> analysisIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> analysisIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> analysisIdSet = analysisList.stream().map(Analysis::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<AnalysisThumb> analysisThumbQueryWrapper = new QueryWrapper<>();
            analysisThumbQueryWrapper.in("analysisId", analysisIdSet);
            analysisThumbQueryWrapper.eq("userId", loginUser.getId());
            List<AnalysisThumb> analysisAnalysisThumbList = analysisThumbMapper.selectList(analysisThumbQueryWrapper);
            analysisAnalysisThumbList.forEach(analysisAnalysisThumb -> analysisIdHasThumbMap.put(analysisAnalysisThumb.getAnalysisId(), true));
            // 获取收藏
            QueryWrapper<AnalysisFavour> analysisFavourQueryWrapper = new QueryWrapper<>();
            analysisFavourQueryWrapper.in("analysisId", analysisIdSet);
            analysisFavourQueryWrapper.eq("userId", loginUser.getId());
            List<AnalysisFavour> analysisFavourList = analysisFavourMapper.selectList(analysisFavourQueryWrapper);
            analysisFavourList.forEach(analysisFavour -> analysisIdHasFavourMap.put(analysisFavour.getAnalysisId(), true));
        }
        // 填充信息
        analysisVOList.forEach(analysisVO -> {
            Long userId = analysisVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            analysisVO.setUser(userService.getUserVO(user));
            analysisVO.setHasThumb(analysisIdHasThumbMap.getOrDefault(analysisVO.getId(), false));
            analysisVO.setHasFavour(analysisIdHasFavourMap.getOrDefault(analysisVO.getId(), false));
        });
        // endregion

        analysisVOPage.setRecords(analysisVOList);
        return analysisVOPage;
    }

}
