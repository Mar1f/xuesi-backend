package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.TeachingPlan;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/15
 **/
public interface TeachingPlanService extends IService<TeachingPlan> {
    /**
     * 根据答题记录生成教案
     * @param userAnswerId 答题记录ID
     * @return 教案ID
     */
    Long generateTeachingPlan(Long userAnswerId);
}