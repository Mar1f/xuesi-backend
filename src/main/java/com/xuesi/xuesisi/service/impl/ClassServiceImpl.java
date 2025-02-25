package com.xuesi.xuesisi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.mapper.ClassMapper;
import com.xuesi.xuesisi.model.dto.clas.ClassQueryRequest;

class.ClassQueryRequest;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.model.entity.ClassFavour;
import com.xuesi.xuesisi.model.entity.ClassThumb;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.ClassVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.ClassService;
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
 * 班级服务实现
 *
 */
@Service
@Slf4j
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param class
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validClass(Class class, boolean add) {
        ThrowUtils.throwIf(class == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = class.getTitle();
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
     * @param classQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Class> getQueryWrapper(ClassQueryRequest classQueryRequest) {
        QueryWrapper<Class> queryWrapper = new QueryWrapper<>();
        if (classQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = classQueryRequest.getId();
        Long notId = classQueryRequest.getNotId();
        String title = classQueryRequest.getTitle();
        String content = classQueryRequest.getContent();
        String searchText = classQueryRequest.getSearchText();
        String sortField = classQueryRequest.getSortField();
        String sortOrder = classQueryRequest.getSortOrder();
        List<String> tagList = classQueryRequest.getTags();
        Long userId = classQueryRequest.getUserId();
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
     * 获取班级封装
     *
     * @param class
     * @param request
     * @return
     */
    @Override
    public ClassVO getClassVO(Class class, HttpServletRequest request) {
        // 对象转封装类
        ClassVO classVO = ClassVO.objToVo(class);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = class.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        classVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long classId = class.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<ClassThumb> classThumbQueryWrapper = new QueryWrapper<>();
            classThumbQueryWrapper.in("classId", classId);
            classThumbQueryWrapper.eq("userId", loginUser.getId());
            ClassThumb classThumb = classThumbMapper.selectOne(classThumbQueryWrapper);
            classVO.setHasThumb(classThumb != null);
            // 获取收藏
            QueryWrapper<ClassFavour> classFavourQueryWrapper = new QueryWrapper<>();
            classFavourQueryWrapper.in("classId", classId);
            classFavourQueryWrapper.eq("userId", loginUser.getId());
            ClassFavour classFavour = classFavourMapper.selectOne(classFavourQueryWrapper);
            classVO.setHasFavour(classFavour != null);
        }
        // endregion

        return classVO;
    }

    /**
     * 分页获取班级封装
     *
     * @param classPage
     * @param request
     * @return
     */
    @Override
    public Page<ClassVO> getClassVOPage(Page<Class> classPage, HttpServletRequest request) {
        List<Class> classList = classPage.getRecords();
        Page<ClassVO> classVOPage = new Page<>(classPage.getCurrent(), classPage.getSize(), classPage.getTotal());
        if (CollUtil.isEmpty(classList)) {
            return classVOPage;
        }
        // 对象列表 => 封装对象列表
        List<ClassVO> classVOList = classList.stream().map(class -> {
            return ClassVO.objToVo(class);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = classList.stream().map(Class::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> classIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> classIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> classIdSet = classList.stream().map(Class::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<ClassThumb> classThumbQueryWrapper = new QueryWrapper<>();
            classThumbQueryWrapper.in("classId", classIdSet);
            classThumbQueryWrapper.eq("userId", loginUser.getId());
            List<ClassThumb> classClassThumbList = classThumbMapper.selectList(classThumbQueryWrapper);
            classClassThumbList.forEach(classClassThumb -> classIdHasThumbMap.put(classClassThumb.getClassId(), true));
            // 获取收藏
            QueryWrapper<ClassFavour> classFavourQueryWrapper = new QueryWrapper<>();
            classFavourQueryWrapper.in("classId", classIdSet);
            classFavourQueryWrapper.eq("userId", loginUser.getId());
            List<ClassFavour> classFavourList = classFavourMapper.selectList(classFavourQueryWrapper);
            classFavourList.forEach(classFavour -> classIdHasFavourMap.put(classFavour.getClassId(), true));
        }
        // 填充信息
        classVOList.forEach(classVO -> {
            Long userId = classVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            classVO.setUser(userService.getUserVO(user));
            classVO.setHasThumb(classIdHasThumbMap.getOrDefault(classVO.getId(), false));
            classVO.setHasFavour(classIdHasFavourMap.getOrDefault(classVO.getId(), false));
        });
        // endregion

        classVOPage.setRecords(classVOList);
        return classVOPage;
    }

}
