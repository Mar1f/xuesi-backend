package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.ClassMapper;
import com.xuesi.xuesisi.mapper.StudentClassMapper;
import com.xuesi.xuesisi.mapper.TeacherClassMapper;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.model.entity.StudentClass;
import com.xuesi.xuesisi.model.entity.TeacherClass;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.vo.ClassVO;
import com.xuesi.xuesisi.service.ClassService;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author mar1
* @description 针对表【class(班级表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    @Resource
    private StudentClassMapper studentClassMapper;

    @Resource
    private TeacherClassMapper teacherClassMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClass(String className, Long teacherId, String description) {
        // 创建班级
        Class classEntity = new Class();
        classEntity.setClassName(className);
        classEntity.setTeacherId(teacherId);
        classEntity.setDescription(description);
        save(classEntity);
        
        // 添加班主任到教师-班级关联表
        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setClassId(classEntity.getId());
        teacherClass.setTeacherId(teacherId);
        teacherClass.setSubject("班主任");
        teacherClassMapper.insert(teacherClass);
        
        return classEntity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addStudentToClass(Long classId, Long studentId) {
        // 检查是否已经存在关联
        LambdaQueryWrapper<StudentClass> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentClass::getClassId, classId)
                   .eq(StudentClass::getStudentId, studentId);
        if (studentClassMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该学生已在班级中");
        }
        
        // 创建关联
        StudentClass studentClass = new StudentClass();
        studentClass.setClassId(classId);
        studentClass.setStudentId(studentId);
        return studentClassMapper.insert(studentClass) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeStudentFromClass(Long classId, Long studentId) {
        LambdaQueryWrapper<StudentClass> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentClass::getClassId, classId)
                   .eq(StudentClass::getStudentId, studentId);
        return studentClassMapper.delete(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addTeacherToClass(Long classId, Long teacherId, String subject) {
        // 检查是否已经存在关联
        LambdaQueryWrapper<TeacherClass> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeacherClass::getClassId, classId)
                   .eq(TeacherClass::getTeacherId, teacherId)
                   .eq(TeacherClass::getSubject, subject);
        if (teacherClassMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该教师已在此班级任教该科目");
        }
        
        // 创建关联
        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setClassId(classId);
        teacherClass.setTeacherId(teacherId);
        teacherClass.setSubject(subject);
        return teacherClassMapper.insert(teacherClass) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeTeacherFromClass(Long classId, Long teacherId, String subject) {
        LambdaQueryWrapper<TeacherClass> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeacherClass::getClassId, classId)
                   .eq(TeacherClass::getTeacherId, teacherId)
                   .eq(TeacherClass::getSubject, subject);
        return teacherClassMapper.delete(queryWrapper) > 0;
    }

    @Override
    public List<Class> getAllClasses() {
        return list();
    }

    @Override
    public Boolean updateClass(Class classEntity) {
        if (classEntity == null || classEntity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return updateById(classEntity);
    }

    @Override
    public Boolean deleteClass(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return removeById(id);
    }

    @Override
    public Page<ClassVO> listClasses(long current, long size) {
        Page<Class> classPage = this.page(new Page<>(current, size));
        Page<ClassVO> classVOPage = new Page<>(classPage.getCurrent(), classPage.getSize(), classPage.getTotal());
        List<ClassVO> classVOList = classPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        classVOPage.setRecords(classVOList);
        return classVOPage;
    }

    @Override
    public ClassVO getClassById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Class classEntity = this.getById(id);
        if (classEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return convertToVO(classEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transferStudent(Long studentId, Long fromClassId, Long toClassId, Long operatorId) {
        // 参数校验
        if (studentId == null || fromClassId == null || toClassId == null || operatorId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 验证学生是否存在
        User student = userService.getById(studentId);
        if (student == null || !"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学生不存在");
        }

        // 验证原班级和目标班级是否存在
        Class fromClass = this.getById(fromClassId);
        Class toClass = this.getById(toClassId);
        if (fromClass == null || toClass == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "班级不存在");
        }

        // 验证操作者权限
        User operator = userService.getById(operatorId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "操作者不存在");
        }
        if (!"admin".equals(operator.getUserRole()) && !fromClass.getTeacherId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限进行转班操作");
        }

        // 从原班级移除学生
        boolean removeResult = removeStudentFromClass(fromClassId, studentId);
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "从原班级移除学生失败");
        }

        // 将学生添加到目标班级
        boolean addResult = addStudentToClass(toClassId, studentId);
        if (!addResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加到目标班级失败");
        }

        return true;
    }

    /**
     * 将班级实体转换为视图对象
     */
    private ClassVO convertToVO(Class classEntity) {
        if (classEntity == null) {
            return null;
        }
        ClassVO classVO = new ClassVO();
        BeanUtils.copyProperties(classEntity, classVO);
        // TODO: 设置学生和教师信息
        return classVO;
    }
}
