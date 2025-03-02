package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.model.entity.Class;
import com.xuesi.xuesisi.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@RestController
@RequestMapping("/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    // 创建班级
    @PostMapping
    public ResponseEntity<Class> createClass(@RequestBody Class classEntity) {
        Class createdClass = classService.createClass(classEntity);
        return new ResponseEntity<>(createdClass, HttpStatus.CREATED);
    }

    // 根据ID查询班级
    @GetMapping("/{id}")
    public ResponseEntity<Class> getClass(@PathVariable Long id) {
        Class classEntity = classService.getClassById(id);
        if (classEntity != null) {
            return ResponseEntity.ok(classEntity);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有班级
    @GetMapping
    public ResponseEntity<List<Class>> getAllClasses() {
        List<Class> classes = classService.getAllClasses();
        return ResponseEntity.ok(classes);
    }

    // 修改班级信息
    @PutMapping("/{id}")
    public ResponseEntity<Class> updateClass(@PathVariable Long id, @RequestBody Class Class) {
        Class updatedClass = classService.updateClass(id, Class);
        if (updatedClass != null) {
            return ResponseEntity.ok(updatedClass);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除班级
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
}