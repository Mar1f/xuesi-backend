package com.xuesi.xuesisi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuesi.xuesisi.model.entity.Class;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author mar1
* @description 针对表【class(班级表)】的数据库操作Service
* @createDate 2025-03-02 15:47:32
*/
public interface ClassService extends IService<Class> {
    Class createClass(Class classEntity);
    Class getClassById(Long id);
    List<Class> getAllClasses();
    Class updateClass(Long id, Class classEntity);
    void deleteClass(Long id);
    Page<Class> getClassesPage(int pageNumber, int pageSize);
}
