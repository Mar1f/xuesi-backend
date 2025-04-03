package com.xuesi.xuesisi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 学习分析统计表
 * @TableName learning_analysis
 */
@TableName(value ="learning_analysis")
@Data
public class LearningAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 班级ID
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 累计总分
     */
    @TableField("total_score")
    private Integer totalScore;


    /**
     * 薄弱知识点ID集合（JSON数组）
     */
    @TableField("weak_tags")
    private String weakTags;

    /**
     * 标签统计（如{"编程": {"correct": 5, "total": 10}}）
     */
    @TableField("tag_stats")
    private String tagStats;

    /**
     * 知识点统计（包含知识点详情和正确率）
     */
    @TableField("knowledge_point_stats")
    private String knowledgePointStats;
    
//    /**
//     * 薄弱知识点ID集合（JSON数组）
//     */
//    @TableField("weak_knowledge_points")
//    private String weakKnowledgePoints;

    /**
     * 题库ID
     */
    @TableField("question_bank_id")
    private Long questionBankId;

    /**
     * 题目ID（JSON数组）
     */
    @TableField("question_id")
    private String questionId;

    /**
     * 用户答案
     */
    @TableField("user_answer")
    private String userAnswer;

    /**
     * 得分
     */
    @TableField("total_score")
    private Integer score;

    /**
     * 分析内容
     */
    private String analysis;

    /**
     * 改进建议
     */
    private String suggestion;

//    /**
//     * 是否为总体评价
//     */
//    @TableField("is_overall")
//    private Boolean isOverall;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField("isDelete")
    @TableLogic
    private Integer isDelete;
}