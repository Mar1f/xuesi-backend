package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.annotation.AuthCheck;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.DeleteRequest;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.constant.UserConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.exception.ThrowUtils;
import com.xuesi.xuesisi.model.dto.clas.ClassAddRequest;
import com.xuesi.xuesisi.model.dto.clas.ClassEditRequest;
import com.xuesi.xuesisi.model.dto.clas.ClassQueryRequest;
import com.xuesi.xuesisi.model.dto.clas.ClassUpdateRequest;

class.ClassAddRequest;
import com.xuesi.xuesisi.model.dto.classes.ClassEditRequest;
import com.xuesi.xuesisi.model.dto.classes.ClassQueryRequest;
import com.xuesi.xuesisi.model.dto.classes.ClassUpdateRequest;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.ClassVO;
import com.xuesi.xuesisi.service.ClassService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 班级接口
 *
 */
@RestController
@RequestMapping("/class")
@Slf4j
public class ClassController {

    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建班级
     *
     * @param classAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addClass(@RequestBody ClassAddRequest classAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(classAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Class class = new Class();
        BeanUtils.copyProperties(classAddRequest, class);
        // 数据校验
        classService.validClass(class, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        class.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = classService.save(class);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newClassId = class.getId();
        return ResultUtils.success(newClassId);
    }

    /**
     * 删除班级
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteClass(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Class oldClass = classService.getById(id);
        ThrowUtils.throwIf(oldClass == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldClass.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = classService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新班级（仅管理员可用）
     *
     * @param classUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateClass(@RequestBody ClassUpdateRequest classUpdateRequest) {
        if (classUpdateRequest == null || classUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Class class = new Class();
        BeanUtils.copyProperties(classUpdateRequest, class);
        // 数据校验
        classService.validClass(class, false);
        // 判断是否存在
        long id = classUpdateRequest.getId();
        Class oldClass = classService.getById(id);
        ThrowUtils.throwIf(oldClass == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = classService.updateById(class);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取班级（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ClassVO> getClassVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Class class = classService.getById(id);
        ThrowUtils.throwIf(class == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(classService.getClassVO(class, request));
    }

    /**
     * 分页获取班级列表（仅管理员可用）
     *
     * @param classQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Class>> listClassByPage(@RequestBody ClassQueryRequest classQueryRequest) {
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        // 查询数据库
        Page<Class> classPage = classService.page(new Page<>(current, size),
                classService.getQueryWrapper(classQueryRequest));
        return ResultUtils.success(classPage);
    }

    /**
     * 分页获取班级列表（封装类）
     *
     * @param classQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ClassVO>> listClassVOByPage(@RequestBody ClassQueryRequest classQueryRequest,
                                                               HttpServletRequest request) {
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Class> classPage = classService.page(new Page<>(current, size),
                classService.getQueryWrapper(classQueryRequest));
        // 获取封装类
        return ResultUtils.success(classService.getClassVOPage(classPage, request));
    }

    /**
     * 分页获取当前登录用户创建的班级列表
     *
     * @param classQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<ClassVO>> listMyClassVOByPage(@RequestBody ClassQueryRequest classQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(classQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        classQueryRequest.setUserId(loginUser.getId());
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Class> classPage = classService.page(new Page<>(current, size),
                classService.getQueryWrapper(classQueryRequest));
        // 获取封装类
        return ResultUtils.success(classService.getClassVOPage(classPage, request));
    }

    /**
     * 编辑班级（给用户使用）
     *
     * @param classEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editClass(@RequestBody ClassEditRequest classEditRequest, HttpServletRequest request) {
        if (classEditRequest == null || classEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Class class = new Class();
        BeanUtils.copyProperties(classEditRequest, class);
        // 数据校验
        classService.validClass(class, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = classEditRequest.getId();
        Class oldClass = classService.getById(id);
        ThrowUtils.throwIf(oldClass == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldClass.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = classService.updateById(class);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
