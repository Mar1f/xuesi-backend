package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.dto.clas.ClassQueryRequest;

class.ClassQueryRequest;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.model.vo.ClassVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 班级服务
 *
 */
public interface ClassService extends IService<Class> {

    /**
     * 校验数据
     *
     * @param class
     * @param add 对创建的数据进行校验
     */
    void validClass(Class class, boolean add);

    /**
     * 获取查询条件
     *
     * @param classQueryRequest
     * @return
     */
    QueryWrapper<Class> getQueryWrapper(ClassQueryRequest classQueryRequest);
    
    /**
     * 获取班级封装
     *
     * @param class
     * @param request
     * @return
     */
    ClassVO getClassVO(Class class, HttpServletRequest request);

    /**
     * 分页获取班级封装
     *
     * @param classPage
     * @param request
     * @return
     */
    Page<ClassVO> getClassVOPage(Page<Class> classPage, HttpServletRequest request);
}
