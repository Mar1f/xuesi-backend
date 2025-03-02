package com.xuesi.xuesisi.controller;



import com.xuesi.xuesisi.model.entity.QuestionBank;
import com.xuesi.xuesisi.service.QuestionBankService;
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
@RequestMapping("/questionBanks")
public class QuestionBankController {

    @Autowired
    private QuestionBankService questionBankService;

    // 创建题单
    @PostMapping
    public ResponseEntity<QuestionBank> createQuestionBank(@RequestBody QuestionBank questionBank) {
        QuestionBank createdQuestionBank = questionBankService.createQuestionBank(questionBank);
        return new ResponseEntity<>(createdQuestionBank, HttpStatus.CREATED);
    }

    // 根据ID查询题单
    @GetMapping("/{id}")
    public ResponseEntity<QuestionBank> getQuestionBank(@PathVariable Long id) {
        QuestionBank questionBank = questionBankService.getQuestionBankById(id);
        if (questionBank != null) {
            return ResponseEntity.ok(questionBank);
        }
        return ResponseEntity.notFound().build();
    }

    // 查询所有题单
    @GetMapping
    public ResponseEntity<List<QuestionBank>> getAllQuestionBanks() {
        List<QuestionBank> questionBanks = questionBankService.getAllQuestionBanks();
        return ResponseEntity.ok(questionBanks);
    }

    // 修改题单
    @PutMapping("/{id}")
    public ResponseEntity<QuestionBank> updateQuestionBank(@PathVariable Long id, @RequestBody QuestionBank questionBank) {
        QuestionBank updatedQuestionBank = questionBankService.updateQuestionBank(id, questionBank);
        if (updatedQuestionBank != null) {
            return ResponseEntity.ok(updatedQuestionBank);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除题单
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable Long id) {
        questionBankService.deleteQuestionBank(id);
        return ResponseEntity.noContent().build();
    }
}