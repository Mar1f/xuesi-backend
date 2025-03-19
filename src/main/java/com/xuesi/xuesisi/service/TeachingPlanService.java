package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.TeachingPlan;

/**
 * 教学计划服务
 */
public interface TeachingPlanService extends IService<TeachingPlan> {
    
    /**
     * 根据用户答案ID获取教学计划
     *
     * @param userAnswerId 用户答案ID
     * @return 教学计划
     */
    TeachingPlan getByUserAnswerId(Long userAnswerId);
    
    /**
     * 根据题库ID获取最新的教学计划
     *
     * @param questionBankId 题库ID
     * @return 教学计划
     */
    TeachingPlan getLatestByQuestionBankId(Long questionBankId);

    /**
     * 根据用户答案ID生成教学计划
     *
     * @param userAnswerId 用户答案ID
     * @return 教学计划ID
     */
    Long generateTeachingPlan(Long userAnswerId);
}