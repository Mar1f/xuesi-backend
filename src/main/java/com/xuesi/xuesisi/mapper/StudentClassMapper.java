package com.xuesi.xuesisi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuesi.xuesisi.model.entity.StudentClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生-班级关联Mapper
 */
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClass> {
} 