package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@RestController
@RequestMapping("/students")
public class StudentController {

    @Resource
    private UserService studentService;

    // 创建学生
    @PostMapping
    public ResponseEntity<User> createStudent(@RequestBody User user) {
        // 确保用户角色为 student
        user.setUserRole("student");
        User createdStudent = studentService.createStudent(user);
        return new ResponseEntity<>(createdStudent, HttpStatus.CREATED);
    }

    // 根据ID查询学生
    @GetMapping("/{id}")
    public ResponseEntity<User> getStudent(@PathVariable Long id) {
        User student = studentService.getStudentById(id);
        if (student != null) {
            return ResponseEntity.ok(student);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有学生
    @GetMapping
    public ResponseEntity<List<User>> getAllStudents() {
        List<User> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    // 修改学生信息
    @PutMapping("/{id}")
    public ResponseEntity<User> updateStudent(@PathVariable Long id, @RequestBody User user) {
        // 确保用户角色保持为 student
        user.setUserRole("student");
        User updatedStudent = studentService.updateStudent(id, user);
        if (updatedStudent != null) {
            return ResponseEntity.ok(updatedStudent);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除学生
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}