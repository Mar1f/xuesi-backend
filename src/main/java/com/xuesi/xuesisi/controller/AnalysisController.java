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
import com.xuesi.xuesisi.model.dto.analysis.AnalysisAddRequest;
import com.xuesi.xuesisi.model.dto.analysis.AnalysisEditRequest;
import com.xuesi.xuesisi.model.dto.analysis.AnalysisQueryRequest;
import com.xuesi.xuesisi.model.dto.analysis.AnalysisUpdateRequest;
import com.xuesi.xuesisi.model.entity.Analysis;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.AnalysisVO;
import com.xuesi.xuesisi.service.AnalysisService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 学习分析接口
 *
 */
@RestController
@RequestMapping("/analysis")
@Slf4j
public class AnalysisController {

    @Resource
    private AnalysisService analysisService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建学习分析
     *
     * @param analysisAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addAnalysis(@RequestBody AnalysisAddRequest analysisAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(analysisAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Analysis analysis = new Analysis();
        BeanUtils.copyProperties(analysisAddRequest, analysis);
        // 数据校验
        analysisService.validAnalysis(analysis, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        analysis.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = analysisService.save(analysis);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newAnalysisId = analysis.getId();
        return ResultUtils.success(newAnalysisId);
    }

    /**
     * 删除学习分析
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAnalysis(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Analysis oldAnalysis = analysisService.getById(id);
        ThrowUtils.throwIf(oldAnalysis == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldAnalysis.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = analysisService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新学习分析（仅管理员可用）
     *
     * @param analysisUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAnalysis(@RequestBody AnalysisUpdateRequest analysisUpdateRequest) {
        if (analysisUpdateRequest == null || analysisUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Analysis analysis = new Analysis();
        BeanUtils.copyProperties(analysisUpdateRequest, analysis);
        // 数据校验
        analysisService.validAnalysis(analysis, false);
        // 判断是否存在
        long id = analysisUpdateRequest.getId();
        Analysis oldAnalysis = analysisService.getById(id);
        ThrowUtils.throwIf(oldAnalysis == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = analysisService.updateById(analysis);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取学习分析（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<AnalysisVO> getAnalysisVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Analysis analysis = analysisService.getById(id);
        ThrowUtils.throwIf(analysis == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(analysisService.getAnalysisVO(analysis, request));
    }

    /**
     * 分页获取学习分析列表（仅管理员可用）
     *
     * @param analysisQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Analysis>> listAnalysisByPage(@RequestBody AnalysisQueryRequest analysisQueryRequest) {
        long current = analysisQueryRequest.getCurrent();
        long size = analysisQueryRequest.getPageSize();
        // 查询数据库
        Page<Analysis> analysisPage = analysisService.page(new Page<>(current, size),
                analysisService.getQueryWrapper(analysisQueryRequest));
        return ResultUtils.success(analysisPage);
    }

    /**
     * 分页获取学习分析列表（封装类）
     *
     * @param analysisQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AnalysisVO>> listAnalysisVOByPage(@RequestBody AnalysisQueryRequest analysisQueryRequest,
                                                               HttpServletRequest request) {
        long current = analysisQueryRequest.getCurrent();
        long size = analysisQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Analysis> analysisPage = analysisService.page(new Page<>(current, size),
                analysisService.getQueryWrapper(analysisQueryRequest));
        // 获取封装类
        return ResultUtils.success(analysisService.getAnalysisVOPage(analysisPage, request));
    }

    /**
     * 分页获取当前登录用户创建的学习分析列表
     *
     * @param analysisQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AnalysisVO>> listMyAnalysisVOByPage(@RequestBody AnalysisQueryRequest analysisQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(analysisQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        analysisQueryRequest.setUserId(loginUser.getId());
        long current = analysisQueryRequest.getCurrent();
        long size = analysisQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Analysis> analysisPage = analysisService.page(new Page<>(current, size),
                analysisService.getQueryWrapper(analysisQueryRequest));
        // 获取封装类
        return ResultUtils.success(analysisService.getAnalysisVOPage(analysisPage, request));
    }

    /**
     * 编辑学习分析（给用户使用）
     *
     * @param analysisEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editAnalysis(@RequestBody AnalysisEditRequest analysisEditRequest, HttpServletRequest request) {
        if (analysisEditRequest == null || analysisEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Analysis analysis = new Analysis();
        BeanUtils.copyProperties(analysisEditRequest, analysis);
        // 数据校验
        analysisService.validAnalysis(analysis, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = analysisEditRequest.getId();
        Analysis oldAnalysis = analysisService.getById(id);
        ThrowUtils.throwIf(oldAnalysis == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldAnalysis.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = analysisService.updateById(analysis);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
