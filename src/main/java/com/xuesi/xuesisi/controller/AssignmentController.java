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
import com.xuesi.xuesisi.model.dto.assignment.AssignmentAddRequest;
import com.xuesi.xuesisi.model.dto.assignment.AssignmentEditRequest;
import com.xuesi.xuesisi.model.dto.assignment.AssignmentQueryRequest;
import com.xuesi.xuesisi.model.dto.assignment.AssignmentUpdateRequest;
import com.xuesi.xuesisi.model.entity.Assignment;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.AssignmentVO;
import com.xuesi.xuesisi.service.AssignmentService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题单接口
 *
 */
@RestController
@RequestMapping("/assignment")
@Slf4j
public class AssignmentController {

    @Resource
    private AssignmentService assignmentService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建题单
     *
     * @param assignmentAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addAssignment(@RequestBody AssignmentAddRequest assignmentAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(assignmentAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(assignmentAddRequest, assignment);
        // 数据校验
        assignmentService.validAssignment(assignment, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        assignment.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = assignmentService.save(assignment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newAssignmentId = assignment.getId();
        return ResultUtils.success(newAssignmentId);
    }

    /**
     * 删除题单
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAssignment(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Assignment oldAssignment = assignmentService.getById(id);
        ThrowUtils.throwIf(oldAssignment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldAssignment.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = assignmentService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题单（仅管理员可用）
     *
     * @param assignmentUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAssignment(@RequestBody AssignmentUpdateRequest assignmentUpdateRequest) {
        if (assignmentUpdateRequest == null || assignmentUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(assignmentUpdateRequest, assignment);
        // 数据校验
        assignmentService.validAssignment(assignment, false);
        // 判断是否存在
        long id = assignmentUpdateRequest.getId();
        Assignment oldAssignment = assignmentService.getById(id);
        ThrowUtils.throwIf(oldAssignment == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = assignmentService.updateById(assignment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题单（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<AssignmentVO> getAssignmentVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Assignment assignment = assignmentService.getById(id);
        ThrowUtils.throwIf(assignment == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(assignmentService.getAssignmentVO(assignment, request));
    }

    /**
     * 分页获取题单列表（仅管理员可用）
     *
     * @param assignmentQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Assignment>> listAssignmentByPage(@RequestBody AssignmentQueryRequest assignmentQueryRequest) {
        long current = assignmentQueryRequest.getCurrent();
        long size = assignmentQueryRequest.getPageSize();
        // 查询数据库
        Page<Assignment> assignmentPage = assignmentService.page(new Page<>(current, size),
                assignmentService.getQueryWrapper(assignmentQueryRequest));
        return ResultUtils.success(assignmentPage);
    }

    /**
     * 分页获取题单列表（封装类）
     *
     * @param assignmentQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AssignmentVO>> listAssignmentVOByPage(@RequestBody AssignmentQueryRequest assignmentQueryRequest,
                                                               HttpServletRequest request) {
        long current = assignmentQueryRequest.getCurrent();
        long size = assignmentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Assignment> assignmentPage = assignmentService.page(new Page<>(current, size),
                assignmentService.getQueryWrapper(assignmentQueryRequest));
        // 获取封装类
        return ResultUtils.success(assignmentService.getAssignmentVOPage(assignmentPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题单列表
     *
     * @param assignmentQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AssignmentVO>> listMyAssignmentVOByPage(@RequestBody AssignmentQueryRequest assignmentQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(assignmentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        assignmentQueryRequest.setUserId(loginUser.getId());
        long current = assignmentQueryRequest.getCurrent();
        long size = assignmentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Assignment> assignmentPage = assignmentService.page(new Page<>(current, size),
                assignmentService.getQueryWrapper(assignmentQueryRequest));
        // 获取封装类
        return ResultUtils.success(assignmentService.getAssignmentVOPage(assignmentPage, request));
    }

    /**
     * 编辑题单（给用户使用）
     *
     * @param assignmentEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editAssignment(@RequestBody AssignmentEditRequest assignmentEditRequest, HttpServletRequest request) {
        if (assignmentEditRequest == null || assignmentEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(assignmentEditRequest, assignment);
        // 数据校验
        assignmentService.validAssignment(assignment, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = assignmentEditRequest.getId();
        Assignment oldAssignment = assignmentService.getById(id);
        ThrowUtils.throwIf(oldAssignment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldAssignment.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = assignmentService.updateById(assignment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
