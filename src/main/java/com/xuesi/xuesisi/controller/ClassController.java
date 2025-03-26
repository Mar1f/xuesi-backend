package com.xuesi.xuesisi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.common.BaseResponse;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.common.ResultUtils;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.ClassVO;
import com.xuesi.xuesisi.model.dto.CLass.CreateClass;
import com.xuesi.xuesisi.model.dto.CLass.UpdateClass;
import com.xuesi.xuesisi.service.ClassService;
import com.xuesi.xuesisi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 班级接口
 */
@RestController
@RequestMapping("/class")
@Slf4j
public class ClassController {

    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    /**
     * 创建班级
     */
    @PostMapping("/create")
    public BaseResponse<Long> createClass(@RequestBody Class classEntity) {
        if (classEntity == null || classEntity.getClassName() == null || classEntity.getTeacherId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 验证教师是否存在
        User teacher = userService.getById(classEntity.getTeacherId());
        if (teacher == null || !"teacher".equals(teacher.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师不存在");
        }
        
        CreateClass createClass = new CreateClass();
        createClass.setClassName(classEntity.getClassName());
        createClass.setTeacherId(classEntity.getTeacherId().intValue());
        createClass.setGrade(classEntity.getGrade());
        createClass.setDescription(classEntity.getDescription());
        
        Long classId = classService.createClass(createClass);
        return ResultUtils.success(classId);
    }

    /**
     * 更新班级信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateClass(@RequestBody Class classEntity) {
        if (classEntity == null || classEntity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        UpdateClass updateClass = new UpdateClass();
        updateClass.setId(classEntity.getId());
        updateClass.setClassName(classEntity.getClassName());
        updateClass.setTeacherId(classEntity.getTeacherId());
        updateClass.setGrade(classEntity.getGrade());
        updateClass.setDescription(classEntity.getDescription());
        
        boolean result = classService.updateClass(updateClass);
        return ResultUtils.success(result);
    }

    /**
     * 删除班级
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteClass(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = classService.deleteClass(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取班级列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<ClassVO>> listClasses(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        Page<ClassVO> classPage = classService.listClasses(current, size);
        return ResultUtils.success(classPage);
    }

    /**
     * 获取班级详情
     */
    @GetMapping("/get")
    public BaseResponse<ClassVO> getClassById(@RequestParam Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClassVO classVO = classService.getClassById(id);
        return ResultUtils.success(classVO);
    }

    /**
     * 添加学生到班级
     */
    @PostMapping("/addStudent")
    public BaseResponse<Boolean> addStudentToClass(
            @RequestParam Long classId,
            @RequestParam Long studentId) {
        if (classId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = classService.addStudentToClass(classId, studentId);
        return ResultUtils.success(result);
    }

    /**
     * 从班级中移除学生
     */
    @PostMapping("/removeStudent")
    public BaseResponse<Boolean> removeStudentFromClass(
            @RequestParam Long classId,
            @RequestParam Long studentId) {
        if (classId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = classService.removeStudentFromClass(classId, studentId);
        return ResultUtils.success(result);
    }

    /**
     * 添加教师到班级
     */
    @PostMapping("/addTeacher")
    public BaseResponse<Boolean> addTeacherToClass(
            @RequestParam Long classId,
            @RequestParam Long teacherId,
            @RequestParam String subject) {
        if (classId == null || teacherId == null || subject == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = classService.addTeacherToClass(classId, teacherId, subject);
        return ResultUtils.success(result);
    }

    /**
     * 从班级中移除教师
     */
    @PostMapping("/removeTeacher")
    public BaseResponse<Boolean> removeTeacherFromClass(
            @RequestParam Long classId,
            @RequestParam Long teacherId,
            @RequestParam String subject) {
        if (classId == null || teacherId == null || subject == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = classService.removeTeacherFromClass(classId, teacherId, subject);
        return ResultUtils.success(result);
    }

    /**
     * 学生转班
     */
    @PostMapping("/transferStudent")
    public BaseResponse<Boolean> transferStudent(
            @RequestParam Long studentId,
            @RequestParam Long fromClassId,
            @RequestParam Long toClassId,
            HttpServletRequest request) {
        if (studentId == null || fromClassId == null || toClassId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 验证当前用户是否有权限进行转班操作
        Long currentUserId = userService.getLoginUser(request).getId();
        boolean result = classService.transferStudent(studentId, fromClassId, toClassId, currentUserId);
        return ResultUtils.success(result);
    }
}