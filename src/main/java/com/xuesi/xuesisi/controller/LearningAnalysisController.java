package com.xuesi.xuesisi.controller;

import com.xuesi.xuesisi.model.entity.LearningAnalysis;
import com.xuesi.xuesisi.service.LearningAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @description；
 * @author:mar1
 * @data:2025/03/02
 **/
@RestController
@RequestMapping("/learningAnalysis")
public class LearningAnalysisController {

    @Autowired
    private LearningAnalysisService learningAnalysisService;

    // 创建学情分析记录
    @PostMapping
    public ResponseEntity<LearningAnalysis> createLearningAnalysis(@RequestBody LearningAnalysis analysis) {
        LearningAnalysis createdAnalysis = learningAnalysisService.createLearningAnalysis(analysis);
        return new ResponseEntity<>(createdAnalysis, HttpStatus.CREATED);
    }

    // 根据 userId 和 classId 查询学情分析记录
    @GetMapping("/{userId}/{classId}")
    public ResponseEntity<LearningAnalysis> getLearningAnalysis(@PathVariable Long userId, @PathVariable Long classId) {
        LearningAnalysis analysis = learningAnalysisService.getLearningAnalysis(userId, classId);
        if (analysis != null) {
            return ResponseEntity.ok(analysis);
        }
        return ResponseEntity.notFound().build();
    }

    // 修改学情分析记录
    @PutMapping("/{userId}/{classId}")
    public ResponseEntity<LearningAnalysis> updateLearningAnalysis(@PathVariable Long userId, @PathVariable Long classId,
                                                                   @RequestBody LearningAnalysis analysis) {
        LearningAnalysis updatedAnalysis = learningAnalysisService.updateLearningAnalysis(userId, classId, analysis);
        if (updatedAnalysis != null) {
            return ResponseEntity.ok(updatedAnalysis);
        }
        return ResponseEntity.notFound().build();
    }

    // 删除学情分析记录
    @DeleteMapping("/{userId}/{classId}")
    public ResponseEntity<Void> deleteLearningAnalysis(@PathVariable Long userId, @PathVariable Long classId) {
        learningAnalysisService.deleteLearningAnalysis(userId, classId);
        return ResponseEntity.noContent().build();
    }
}