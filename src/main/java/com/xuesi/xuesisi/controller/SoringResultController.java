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
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultAddRequest;
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultEditRequest;
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultQueryRequest;
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultUpdateRequest;
import com.xuesi.xuesisi.model.entity.SoringResult;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.SoringResultVO;
import com.xuesi.xuesisi.service.SoringResultService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 评分结果接口
 *
 */
@RestController
@RequestMapping("/soringResult")
@Slf4j
public class SoringResultController {

    @Resource
    private SoringResultService soringResultService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建评分结果
     *
     * @param soringResultAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSoringResult(@RequestBody SoringResultAddRequest soringResultAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(soringResultAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        SoringResult soringResult = new SoringResult();
        BeanUtils.copyProperties(soringResultAddRequest, soringResult);
        // 数据校验
        soringResultService.validSoringResult(soringResult, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        soringResult.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = soringResultService.save(soringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newSoringResultId = soringResult.getId();
        return ResultUtils.success(newSoringResultId);
    }

    /**
     * 删除评分结果
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSoringResult(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        SoringResult oldSoringResult = soringResultService.getById(id);
        ThrowUtils.throwIf(oldSoringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldSoringResult.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = soringResultService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新评分结果（仅管理员可用）
     *
     * @param soringResultUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSoringResult(@RequestBody SoringResultUpdateRequest soringResultUpdateRequest) {
        if (soringResultUpdateRequest == null || soringResultUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        SoringResult soringResult = new SoringResult();
        BeanUtils.copyProperties(soringResultUpdateRequest, soringResult);
        // 数据校验
        soringResultService.validSoringResult(soringResult, false);
        // 判断是否存在
        long id = soringResultUpdateRequest.getId();
        SoringResult oldSoringResult = soringResultService.getById(id);
        ThrowUtils.throwIf(oldSoringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = soringResultService.updateById(soringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取评分结果（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SoringResultVO> getSoringResultVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SoringResult soringResult = soringResultService.getById(id);
        ThrowUtils.throwIf(soringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(soringResultService.getSoringResultVO(soringResult, request));
    }

    /**
     * 分页获取评分结果列表（仅管理员可用）
     *
     * @param soringResultQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SoringResult>> listSoringResultByPage(@RequestBody SoringResultQueryRequest soringResultQueryRequest) {
        long current = soringResultQueryRequest.getCurrent();
        long size = soringResultQueryRequest.getPageSize();
        // 查询数据库
        Page<SoringResult> soringResultPage = soringResultService.page(new Page<>(current, size),
                soringResultService.getQueryWrapper(soringResultQueryRequest));
        return ResultUtils.success(soringResultPage);
    }

    /**
     * 分页获取评分结果列表（封装类）
     *
     * @param soringResultQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SoringResultVO>> listSoringResultVOByPage(@RequestBody SoringResultQueryRequest soringResultQueryRequest,
                                                               HttpServletRequest request) {
        long current = soringResultQueryRequest.getCurrent();
        long size = soringResultQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<SoringResult> soringResultPage = soringResultService.page(new Page<>(current, size),
                soringResultService.getQueryWrapper(soringResultQueryRequest));
        // 获取封装类
        return ResultUtils.success(soringResultService.getSoringResultVOPage(soringResultPage, request));
    }

    /**
     * 分页获取当前登录用户创建的评分结果列表
     *
     * @param soringResultQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<SoringResultVO>> listMySoringResultVOByPage(@RequestBody SoringResultQueryRequest soringResultQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(soringResultQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        soringResultQueryRequest.setUserId(loginUser.getId());
        long current = soringResultQueryRequest.getCurrent();
        long size = soringResultQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<SoringResult> soringResultPage = soringResultService.page(new Page<>(current, size),
                soringResultService.getQueryWrapper(soringResultQueryRequest));
        // 获取封装类
        return ResultUtils.success(soringResultService.getSoringResultVOPage(soringResultPage, request));
    }

    /**
     * 编辑评分结果（给用户使用）
     *
     * @param soringResultEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSoringResult(@RequestBody SoringResultEditRequest soringResultEditRequest, HttpServletRequest request) {
        if (soringResultEditRequest == null || soringResultEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        SoringResult soringResult = new SoringResult();
        BeanUtils.copyProperties(soringResultEditRequest, soringResult);
        // 数据校验
        soringResultService.validSoringResult(soringResult, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = soringResultEditRequest.getId();
        SoringResult oldSoringResult = soringResultService.getById(id);
        ThrowUtils.throwIf(oldSoringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldSoringResult.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = soringResultService.updateById(soringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
