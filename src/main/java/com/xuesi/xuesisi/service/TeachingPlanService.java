package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.entity.TeachingPlan;

import java.util.List;

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

    /**
     * 获取教案列表
     *
     * @param userId 用户ID（可选）
     * @param questionBankId 题库ID（可选）
     * @return 教案列表
     */
    List<TeachingPlan> list(Long userId, Long questionBankId);

    /**
     * 分页获取教案
     *
     * @param current 当前页
     * @param size 每页大小
     * @param userId 用户ID（可选）
     * @param questionBankId 题库ID（可选）
     * @return 分页结果
     */
    Page<TeachingPlan> page(long current, long size, Long userId, Long questionBankId);

    /**
     * 创建教案
     *
     * @param teachingPlan 教案信息
     * @return 创建的教案
     */
    TeachingPlan createTeachingPlan(TeachingPlan teachingPlan);

    /**
     * 更新教案
     *
     * @param teachingPlan 教案信息
     * @return 更新后的教案
     */
    TeachingPlan updateTeachingPlan(TeachingPlan teachingPlan);
}