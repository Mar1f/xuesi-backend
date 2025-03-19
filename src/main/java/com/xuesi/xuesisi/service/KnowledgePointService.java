package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;

import java.util.List;

/**
 * 知识点服务接口
 */
public interface KnowledgePointService extends IService<KnowledgePoint> {
    
    /**
     * 创建知识点
     */
    Long createKnowledgePoint(KnowledgePoint knowledgePoint);
    
    /**
     * 修改知识点
     */
    boolean updateKnowledgePoint(KnowledgePoint knowledgePoint);
    
    /**
     * 删除知识点
     */
    boolean deleteKnowledgePoint(Long id);
    
    /**
     * 获取知识点详情
     */
    KnowledgePoint getKnowledgePointById(Long id);
    
    /**
     * 分页获取知识点列表
     */
    Page<KnowledgePoint> listKnowledgePoints(Long userId, String subject, int pageNum, int pageSize);
    
    /**
     * 根据学科获取所有知识点
     */
    List<KnowledgePoint> getKnowledgePointsBySubject(String subject);
    
    /**
     * 批量检查知识点是否存在，如不存在则创建
     * 返回知识点ID列表
     */
    List<Long> getOrCreateKnowledgePoints(List<String> knowledgeNames, String subject, Long userId);
}
