package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.KnowledgePointMapper;
import com.xuesi.xuesisi.model.entity.KnowledgePoint;
import com.xuesi.xuesisi.service.KnowledgePointService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识点服务实现类
 */
@Service
@Slf4j
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> implements KnowledgePointService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgePoint(KnowledgePoint knowledgePoint) {
        if (knowledgePoint == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 检查必须参数
        if (StringUtils.isBlank(knowledgePoint.getName()) || StringUtils.isBlank(knowledgePoint.getSubject())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识点名称和学科不能为空");
        }
        
        // 验证同一学科下知识点名称是否重复
        long count = this.count(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getName, knowledgePoint.getName())
                .eq(KnowledgePoint::getSubject, knowledgePoint.getSubject()));
                
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该学科下已存在同名知识点");
        }
        
        // 保存知识点
        boolean result = this.save(knowledgePoint);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "知识点创建失败");
        }
        
        return knowledgePoint.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateKnowledgePoint(KnowledgePoint knowledgePoint) {
        if (knowledgePoint == null || knowledgePoint.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 检查知识点是否存在
        KnowledgePoint original = getById(knowledgePoint.getId());
        if (original == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识点不存在");
        }
        
        // 如果修改了名称，检查是否重复
        if (StringUtils.isNotBlank(knowledgePoint.getName()) 
                && !original.getName().equals(knowledgePoint.getName())) {
            long count = this.count(Wrappers.<KnowledgePoint>lambdaQuery()
                    .eq(KnowledgePoint::getName, knowledgePoint.getName())
                    .eq(KnowledgePoint::getSubject, original.getSubject())
                    .ne(KnowledgePoint::getId, knowledgePoint.getId()));
                    
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该学科下已存在同名知识点");
            }
        }
        
        return this.updateById(knowledgePoint);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledgePoint(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 检查知识点是否存在
        KnowledgePoint knowledgePoint = getById(id);
        if (knowledgePoint == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识点不存在");
        }
        
        // 删除知识点
        return this.removeById(id);
    }
    
    @Override
    public KnowledgePoint getKnowledgePointById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return getById(id);
    }
    
    @Override
    public Page<KnowledgePoint> listKnowledgePoints(Long userId, String subject, int pageNum, int pageSize) {
        LambdaQueryWrapper<KnowledgePoint> queryWrapper = Wrappers.lambdaQuery();
        
        // 根据条件查询
        if (userId != null) {
            queryWrapper.eq(KnowledgePoint::getUserId, userId);
        }
        
        if (StringUtils.isNotBlank(subject)) {
            queryWrapper.eq(KnowledgePoint::getSubject, subject);
        }
        
        // 按名称排序
        queryWrapper.orderByAsc(KnowledgePoint::getName);
        
        return this.page(new Page<>(pageNum, pageSize), queryWrapper);
    }
    
    @Override
    public List<KnowledgePoint> getKnowledgePointsBySubject(String subject) {
        if (StringUtils.isBlank(subject)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学科不能为空");
        }
        
        return this.list(Wrappers.<KnowledgePoint>lambdaQuery()
                .eq(KnowledgePoint::getSubject, subject)
                .orderByAsc(KnowledgePoint::getName));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> getOrCreateKnowledgePoints(List<String> knowledgeNames, String subject, Long userId) {
        if (knowledgeNames == null || knowledgeNames.isEmpty() || StringUtils.isBlank(subject)) {
            return new ArrayList<>();
        }
        
        List<Long> knowledgeIds = new ArrayList<>();
        
        for (String name : knowledgeNames) {
            // 处理空字符串
            if (StringUtils.isBlank(name)) {
                continue;
            }
            
            // 查询是否已存在
            KnowledgePoint existingPoint = getOne(Wrappers.<KnowledgePoint>lambdaQuery()
                    .eq(KnowledgePoint::getName, name.trim())
                    .eq(KnowledgePoint::getSubject, subject));
                    
            // 如果已存在，使用现有ID
            if (existingPoint != null) {
                knowledgeIds.add(existingPoint.getId());
                continue;
            }
            
            // 不存在则创建新知识点
            KnowledgePoint newPoint = new KnowledgePoint();
            newPoint.setName(name.trim());
            newPoint.setSubject(subject);
            newPoint.setUserId(userId);
            
            try {
                this.save(newPoint);
                knowledgeIds.add(newPoint.getId());
                log.info("已创建新知识点：{}, ID={}", name, newPoint.getId());
            } catch (Exception e) {
                log.error("创建知识点失败：{}", name, e);
                // 继续处理其他知识点
            }
        }
        
        return knowledgeIds.stream().distinct().collect(Collectors.toList());
    }
}




