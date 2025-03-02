package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.dto.assignment.AssignmentQueryRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * 题单服务
 *
 */
public interface AssignmentService extends IService<Assignment> {

    /**
     * 校验数据
     *
     * @param assignment
     * @param add 对创建的数据进行校验
     */
    void validAssignment(Assignment assignment, boolean add);

    /**
     * 获取查询条件
     *
     * @param assignmentQueryRequest
     * @return
     */
    QueryWrapper<Assignment> getQueryWrapper(AssignmentQueryRequest assignmentQueryRequest);
    
    /**
     * 获取题单封装
     *
     * @param assignment
     * @param request
     * @return
     */
    AssignmentVO getAssignmentVO(Assignment assignment, HttpServletRequest request);

    /**
     * 分页获取题单封装
     *
     * @param assignmentPage
     * @param request
     * @return
     */
    Page<AssignmentVO> getAssignmentVOPage(Page<Assignment> assignmentPage, HttpServletRequest request);
}
