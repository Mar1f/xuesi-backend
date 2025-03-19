package com.xuesi.xuesisi.model.entity;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import lombok.Data;

/**
 * 题目表
 * @TableName question
 */
@TableName(value ="question")
@Data
public class Question implements Serializable {
    /**
     * 题目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题干文本
     */
    private String questionContent;

    /**
     * 标签列表（json 数组）
     */
    @TableField(value = "tags")
    private String tagsStr;

    @TableField(exist = false)
    private List<String> tags;

    /**
     * 关联的知识点列表
     */
    @TableField(exist = false)
    private List<KnowledgePoint> knowledgePoints;

    /**
     * 题型: 0-单选, 1-多选, 2-填空
     */
    private Integer questionType;

    /**
     * 选项列表
     */
    @TableField(value = "options")
    private String optionsStr;

    @TableField(exist = false)
    private List<String> options;

    /**
     * 正确答案
     */
    @TableField(value = "answer")
    private String answerStr;

    @TableField(exist = false)
    private List<String> answer;

    /**
     * 题目分值
     */
    private Integer score;

    /**
     * 来源: 0-手动, 1-AI生成
     */
    private Integer source;

    /**
     * 题目解析
     */
    private String analysis;

    /**
     * 参考答案（用于简答题）
     */
    private String referenceAnswer;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public void setOptions(List<String> options) {
        this.options = options;
        this.optionsStr = options != null ? JSONUtil.toJsonStr(options) : null;
    }

    public List<String> getOptions() {
        if (this.options == null && this.optionsStr != null) {
            try {
                // 如果 optionsStr 不是以 [ 开头，说明是旧格式，需要转换为数组格式
                if (!this.optionsStr.trim().startsWith("[")) {
                    // 将逗号分隔的字符串转换为数组
                    String[] optionArray = this.optionsStr.split(",");
                    this.options = Arrays.asList(optionArray);
                    // 更新数据库中的格式
                    this.optionsStr = JSONUtil.toJsonStr(this.options);
                } else {
                    this.options = JSONUtil.toList(this.optionsStr, String.class);
                }
            } catch (Exception e) {
                // 如果解析失败，返回空列表
                this.options = new ArrayList<>();
                this.optionsStr = "[]";
            }
        }
        return this.options;
    }

    public void setAnswer(List<String> answer) {
        this.answer = answer;
        this.answerStr = answer != null ? JSONUtil.toJsonStr(answer) : null;
    }

    public List<String> getAnswer() {
        if (this.answer == null && this.answerStr != null) {
            try {
                // 如果 answerStr 不是以 [ 开头，说明是旧格式，需要转换为数组格式
                if (!this.answerStr.trim().startsWith("[")) {
                    // 将逗号分隔的字符串转换为数组
                    String[] answerArray = this.answerStr.split(",");
                    this.answer = Arrays.asList(answerArray);
                    // 更新数据库中的格式
                    this.answerStr = JSONUtil.toJsonStr(this.answer);
                } else {
                    this.answer = JSONUtil.toList(this.answerStr, String.class);
                }
            } catch (Exception e) {
                // 如果解析失败，返回空列表
                this.answer = new ArrayList<>();
                this.answerStr = "[]";
            }
        }
        return this.answer;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        this.tagsStr = tags != null ? JSONUtil.toJsonStr(tags) : null;
    }

    public List<String> getTags() {
        if (this.tags == null && this.tagsStr != null) {
            try {
                // 如果 tagsStr 不是以 [ 开头，说明是旧格式，需要转换为数组格式
                if (!this.tagsStr.trim().startsWith("[")) {
                    // 将逗号分隔的字符串转换为数组
                    String[] tagArray = this.tagsStr.split(",");
                    this.tags = Arrays.asList(tagArray);
                    // 更新数据库中的格式
                    this.tagsStr = JSONUtil.toJsonStr(this.tags);
                } else {
                    this.tags = JSONUtil.toList(this.tagsStr, String.class);
                }
            } catch (Exception e) {
                // 如果解析失败，返回空列表
                this.tags = new ArrayList<>();
                this.tagsStr = "[]";
            }
        }
        return this.tags;
    }
}