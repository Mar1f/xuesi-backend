package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.Class;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xuesi.xuesisi.model.vo.ClassVO;
import com.xuesi.xuesisi.model.dto.CLass.CreateClass;
import com.xuesi.xuesisi.model.dto.CLass.QueryClass;
import com.xuesi.xuesisi.model.dto.CLass.QueryClassRequest;
import com.xuesi.xuesisi.model.dto.CLass.UpdateClass;

import java.util.List;

/**
* @author mar1
* @description 针对表【class(班级表)】的数据库操作Service
* @createDate 2025-03-02 15:47:32
*/
public interface ClassService extends IService<Class> {
    /**
     * 创建班级
     */
    Long createClass(CreateClass createClass);

    /**
     * 添加学生到班级
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 是否添加成功
     */
    Boolean addStudentToClass(Long classId, Long studentId);

    /**
     * 从班级移除学生
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 是否移除成功
     */
    Boolean removeStudentFromClass(Long classId, Long studentId);

    /**
     * 添加教师到班级
     * @param classId 班级ID
     * @param teacherId 教师ID
     * @param subject 任教科目
     * @return 是否添加成功
     */
    Boolean addTeacherToClass(Long classId, Long teacherId, String subject);

    /**
     * 从班级移除教师
     * @param classId 班级ID
     * @param teacherId 教师ID
     * @param subject 任教科目
     * @return 是否移除成功
     */
    Boolean removeTeacherFromClass(Long classId, Long teacherId, String subject);

    /**
     * 获取所有班级列表
     */
    List<Class> getAllClasses();

    /**
     * 更新班级
     */
    Boolean updateClass(UpdateClass updateClass);

    /**
     * 删除班级
     */
    Boolean deleteClass(Long id);

    /**
     * 分页获取班级列表
     */
    Page<ClassVO> listClasses(long current, long size);

    /**
     * 查询班级列表
     */
    List<QueryClass> queryClassList(QueryClassRequest request);

    /**
     * 获取班级详情
     */
    QueryClass getClassDetail(Long id);

    /**
     * 根据ID获取班级信息
     */
    ClassVO getClassById(Long id);

    /**
     * 学生转班
     *
     * @param studentId 学生ID
     * @param fromClassId 原班级ID
     * @param toClassId 目标班级ID
     * @param operatorId 操作者ID
     * @return 是否转班成功
     */
    Boolean transferStudent(Long studentId, Long fromClassId, Long toClassId, Long operatorId);
}
