package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.model.entity.Question;
import com.xuesi.xuesisi.service.QuestionService;
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
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    // 创建题目
    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question createdQuestion = questionService.createQuestion(question);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    // 根据ID查询题目
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        if (question != null) {
            return ResponseEntity.ok(question);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有题目
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }

    // 修改题目
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        Question updatedQuestion = questionService.updateQuestion(id, question);
        if (updatedQuestion != null) {
            return ResponseEntity.ok(updatedQuestion);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除题目
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}