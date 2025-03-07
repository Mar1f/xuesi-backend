package com.xuesi.xuesisi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuesi.xuesisi.model.entity.TeacherClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师-班级关联Mapper
 */
@Mapper
public interface TeacherClassMapper extends BaseMapper<TeacherClass> {
} 