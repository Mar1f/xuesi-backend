package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.service.ClassService;
import com.xuesi.xuesisi.mapper.ClassMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author mar1
* @description 针对表【class(班级表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    @Override
    public Class createClass(Class classEntity) {
        save(classEntity);
        return classEntity;
    }

    @Override
    public Class getClassById(Long id) {
        return getById(id);
    }

    @Override
    public List<Class> getAllClasses() {
        return list();
    }

    @Override
    public Class updateClass(Long id, Class classEntity) {
        classEntity.setId(id);
        if (updateById(classEntity)) {
            return getById(id);
        }
        return null;
    }

    @Override
    public void deleteClass(Long id) {
        removeById(id);
    }

    @Override
    public Page<Class> getClassesPage(int pageNumber, int pageSize) {
        Page<Class> page = new Page<>(pageNumber, pageSize);
        return page(page);
    }
}
