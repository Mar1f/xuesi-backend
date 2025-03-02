package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.dto.analysis.AnalysisQueryRequest;
import com.xuesi.xuesisi.model.entity.Analysis;

import javax.servlet.http.HttpServletRequest;

/**
 * 学习分析服务
 *
 */
public interface AnalysisService extends IService<Analysis> {

    /**
     * 校验数据
     *
     * @param analysis
     * @param add 对创建的数据进行校验
     */
    void validAnalysis(Analysis analysis, boolean add);

    /**
     * 获取查询条件
     *
     * @param analysisQueryRequest
     * @return
     */
    QueryWrapper<Analysis> getQueryWrapper(AnalysisQueryRequest analysisQueryRequest);
    
    /**
     * 获取学习分析封装
     *
     * @param analysis
     * @param request
     * @return
     */
    AnalysisVO getAnalysisVO(Analysis analysis, HttpServletRequest request);

    /**
     * 分页获取学习分析封装
     *
     * @param analysisPage
     * @param request
     * @return
     */
    Page<AnalysisVO> getAnalysisVOPage(Page<Analysis> analysisPage, HttpServletRequest request);
}
