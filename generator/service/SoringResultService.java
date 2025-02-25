package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.dto.soringResult.SoringResultQueryRequest;
import com.xuesi.xuesisi.model.entity.SoringResult;
import com.xuesi.xuesisi.model.vo.SoringResultVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 评分结果服务
 *
 */
public interface SoringResultService extends IService<SoringResult> {

    /**
     * 校验数据
     *
     * @param soringResult
     * @param add 对创建的数据进行校验
     */
    void validSoringResult(SoringResult soringResult, boolean add);

    /**
     * 获取查询条件
     *
     * @param soringResultQueryRequest
     * @return
     */
    QueryWrapper<SoringResult> getQueryWrapper(SoringResultQueryRequest soringResultQueryRequest);
    
    /**
     * 获取评分结果封装
     *
     * @param soringResult
     * @param request
     * @return
     */
    SoringResultVO getSoringResultVO(SoringResult soringResult, HttpServletRequest request);

    /**
     * 分页获取评分结果封装
     *
     * @param soringResultPage
     * @param request
     * @return
     */
    Page<SoringResultVO> getSoringResultVOPage(Page<SoringResult> soringResultPage, HttpServletRequest request);
}
