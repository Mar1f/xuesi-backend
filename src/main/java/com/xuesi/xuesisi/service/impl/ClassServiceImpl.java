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
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.model.dto.CLass.CreateClass;
import com.xuesi.xuesisi.model.dto.CLass.QueryClass;
import com.xuesi.xuesisi.model.dto.CLass.QueryClassRequest;
import com.xuesi.xuesisi.model.dto.CLass.UpdateClass;
import com.xuesi.xuesisi.service.ClassService;
import com.xuesi.xuesisi.service.UserService;
import org.apache.commons.lang3.StringUtils;
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
    public Long createClass(CreateClass createClass) {
        if (createClass == null || createClass.getClassName() == null || createClass.getTeacherId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 验证教师是否存在
        User teacher = userService.getById(createClass.getTeacherId());
        if (teacher == null || !"teacher".equals(teacher.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师不存在");
        }
        
        Class classEntity = new Class();
        classEntity.setClassName(createClass.getClassName());
        classEntity.setTeacherId(createClass.getTeacherId().longValue());
        classEntity.setGrade(createClass.getGrade());
        classEntity.setDescription(createClass.getDescription());
        
        boolean success = this.save(classEntity);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
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
    public Boolean updateClass(UpdateClass updateClass) {
        if (updateClass == null || updateClass.getTeacherId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        Class classEntity = new Class();
        classEntity.setId(updateClass.getId());
        classEntity.setClassName(updateClass.getClassName());
        classEntity.setTeacherId(updateClass.getTeacherId());
        classEntity.setGrade(updateClass.getGrade());
        classEntity.setDescription(updateClass.getDescription());
        
        return this.updateById(classEntity);
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
        classVO.setId(classEntity.getId());
        classVO.setName(classEntity.getClassName());
        classVO.setDescription(classEntity.getDescription());
        classVO.setGrade(classEntity.getGrade());
        classVO.setHeadTeacherId(classEntity.getTeacherId());
        classVO.setCreateTime(java.util.Date.from(classEntity.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        classVO.setUpdateTime(java.util.Date.from(classEntity.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        classVO.setIsDelete(classEntity.getIsDelete());
        
        // 获取班主任信息
        User headTeacher = userService.getById(classEntity.getTeacherId());
        if (headTeacher != null) {
            classVO.setHeadTeacherName(headTeacher.getUserName());
        }
        
        // 获取学生列表
        LambdaQueryWrapper<StudentClass> studentQueryWrapper = new LambdaQueryWrapper<>();
        studentQueryWrapper.eq(StudentClass::getClassId, classEntity.getId());
        List<StudentClass> studentClasses = studentClassMapper.selectList(studentQueryWrapper);
        List<Long> studentIds = studentClasses.stream()
                .map(StudentClass::getStudentId)
                .collect(Collectors.toList());
        classVO.setStudentIds(studentIds);
        
        // 如果有学生ID，获取学生详细信息
        if (!studentIds.isEmpty()) {
            List<User> students = userService.listByIds(studentIds);
            List<UserVO> studentVOs = students.stream()
                    .filter(student -> student.getIsDelete() == null || student.getIsDelete() == 0)
                    .map(student -> {
                        UserVO userVO = new UserVO();
                        BeanUtils.copyProperties(student, userVO);
                        return userVO;
                    })
                    .collect(Collectors.toList());
            classVO.setStudents(studentVOs);
            classVO.setStudentIds(studentVOs.stream()
                    .map(UserVO::getId)
                    .collect(Collectors.toList()));
        }
        
        // 获取教师列表
        LambdaQueryWrapper<TeacherClass> teacherQueryWrapper = new LambdaQueryWrapper<>();
        teacherQueryWrapper.eq(TeacherClass::getClassId, classEntity.getId());
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(teacherQueryWrapper);
        List<Long> teacherIds = teacherClasses.stream()
                .map(TeacherClass::getTeacherId)
                .collect(Collectors.toList());
        classVO.setTeacherIds(teacherIds);
        
        // 如果有教师ID，获取教师详细信息
        if (!teacherIds.isEmpty()) {
            List<User> teachers = userService.listByIds(teacherIds);
            List<UserVO> teacherVOs = teachers.stream()
                    .filter(teacher -> teacher.getIsDelete() == null || teacher.getIsDelete() == 0)
                    .map(teacher -> {
                        UserVO userVO = new UserVO();
                        BeanUtils.copyProperties(teacher, userVO);
                        return userVO;
                    })
                    .collect(Collectors.toList());
            classVO.setTeachers(teacherVOs);
            classVO.setTeacherIds(teacherVOs.stream()
                    .map(UserVO::getId)
                    .collect(Collectors.toList()));
        }
        
        return classVO;
    }

    @Override
    public List<QueryClass> queryClassList(QueryClassRequest request) {
        LambdaQueryWrapper<Class> queryWrapper = new LambdaQueryWrapper<>();
        
        if (request.getClassName() != null) {
            queryWrapper.like(Class::getClassName, request.getClassName());
        }
        if (request.getGrade() != null) {
            queryWrapper.eq(Class::getGrade, request.getGrade());
        }
        if (request.getTeacherId() != null) {
            queryWrapper.eq(Class::getTeacherId, request.getTeacherId());
        }
        
        List<Class> classList = this.list(queryWrapper);
        return classList.stream().map(classEntity -> {
            QueryClass queryClass = new QueryClass();
            queryClass.setId(classEntity.getId());
            queryClass.setClassName(classEntity.getClassName());
            queryClass.setTeacherId(classEntity.getTeacherId());
            queryClass.setIsDelete(classEntity.getIsDelete() ? 1 : 0);
            return queryClass;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryClass getClassDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        Class classEntity = this.getById(id);
        if (classEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        
        QueryClass queryClass = new QueryClass();
        queryClass.setId(classEntity.getId());
        queryClass.setClassName(classEntity.getClassName());
        queryClass.setTeacherId(classEntity.getTeacherId());
        queryClass.setIsDelete(classEntity.getIsDelete() ? 1 : 0);
        
        return queryClass;
    }

    /**
     * 将Class实体转换为QueryClass对象
     */
    private QueryClass convertToQueryClass(Class classEntity) {
        QueryClass queryClass = new QueryClass();
        queryClass.setId(classEntity.getId());
        queryClass.setClassName(classEntity.getClassName());
        queryClass.setTeacherId(classEntity.getTeacherId());
        queryClass.setIsDelete(classEntity.getIsDelete() ? 1 : 0);
        return queryClass;
    }
}
