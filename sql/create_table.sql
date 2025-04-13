-- ----------------------------
-- 数据库初始化
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `xuesisi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xuesisi`;

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
     `id`           BIGINT AUTO_INCREMENT COMMENT '用户ID' PRIMARY KEY,
     `userAccount`  VARCHAR(256) NOT NULL COMMENT '账号',
    `userPassword` VARCHAR(512) NOT NULL COMMENT '密码',
    `userName`     VARCHAR(256) NULL COMMENT '用户昵称',
    `userAvatar`   VARCHAR(1024) NULL COMMENT '用户头像',
    `userProfile`  VARCHAR(512) NULL COMMENT '用户简介',
    `userRole`     VARCHAR(64)  NOT NULL DEFAULT 'student' COMMENT '用户角色: student/teacher/admin/ban',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`     TINYINT      NOT NULL DEFAULT 0,
    INDEX `idx_userAccount` (`userAccount`)
    ) COMMENT '用户表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 班级表
-- ----------------------------

CREATE TABLE IF NOT EXISTS `class` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `className` varchar(50) NOT NULL COMMENT '班级名称',
    `teacherId` bigint NOT NULL COMMENT '班主任ID',
    `description` varchar(500) DEFAULT NULL COMMENT '班级描述',
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级' ,
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_teacherId` (`teacherId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表';

-- ----------------------------
-- 学生班级关系表（
-- ----------------------------
USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `student_class` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
    KEY `idx_student_id` (`student_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生-班级关联表';
-- ----------------------------
-- 题单表
-- ----------------------------

CREATE TABLE IF NOT EXISTS `teacher_class` (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
     `teacherId` bigint NOT NULL COMMENT '教师ID',
     `classId` bigint NOT NULL COMMENT '班级ID',
     `subject` varchar(50) NOT NULL COMMENT '任教科目',
     `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
     PRIMARY KEY (`id`),
     UNIQUE KEY `uk_teacher_class_subject` (`teacherId`, `classId`, `subject`),
     KEY `idx_teacherId` (`teacherId`),
     KEY `idx_classId` (`classId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师-班级关联表';

USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `question_bank` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '题单ID' PRIMARY KEY,
    `title`            VARCHAR(128)  NOT NULL COMMENT '题单名称',
    `description`      TEXT          NULL COMMENT '描述',
    `picture`          VARCHAR(1024) NULL COMMENT '图标',
    `questionBankType` TINYINT       NOT NULL DEFAULT 0 COMMENT '类型: 0-单选, 1-多选，2-填空，3-简答',
    `scoringStrategy`  TINYINT       NOT NULL DEFAULT 0 COMMENT '评分策略: 0-自定义, 1-AI',
    `totalScore`       INT           NOT NULL DEFAULT 100 COMMENT '题单总分',
    `passScore`        INT           NOT NULL DEFAULT 60 COMMENT '及格分',
    `questionCount`    INT           NOT NULL DEFAULT 0 COMMENT '题目数量',
    `subject`         VARCHAR(64)   NULL COMMENT '学科',
    `classId` bigint NOT NULL COMMENT '所属班级ID',
    `endTime`          DATETIME      NULL COMMENT '截止时间',
    `reviewStatus`     TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审, 1-通过, 2-拒绝',
    `reviewMessage`    VARCHAR(512)  NULL COMMENT '审核信息',
    `reviewerId`       BIGINT        NULL COMMENT '审核人ID',
    `reviewTime`       DATETIME      NULL COMMENT '审核时间',
    `userId`           BIGINT        NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT       NOT NULL DEFAULT 0,
    INDEX `idx_userId` (`userId`)
    ) COMMENT '题单表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题目表
-- ----------------------------
USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `question` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '题目ID' PRIMARY KEY,
    `questionContent`  TEXT           NOT NULL COMMENT '题干文本',
    `tags`             VARCHAR(1024)  NULL COMMENT '标签列表（json 数组）',
    `questionType`     TINYINT        NOT NULL COMMENT '题型: 0-单选, 1-多选, 2-填空',
    `options`          TEXT           NULL COMMENT '选项（JSON数组, 如["A","B"]）',
    `answer`           MEDIUMTEXT            NOT NULL COMMENT '正确答案',
    `score`            INT            NOT NULL DEFAULT 10 COMMENT '题目分值',
    `source`           TINYINT        NOT NULL DEFAULT 0 COMMENT '来源: 0-手动, 1-AI生成',
    `analysis`         TEXT           NULL COMMENT '题目解析',
    `referenceAnswer` TEXT NULL COMMENT '参考答案（用于简答题）',
    `userId`           BIGINT         NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT        NOT NULL DEFAULT 0,
    INDEX `idx_userId` (`userId`)
    ) COMMENT '题目表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题库题目表
-- ----------------------------
USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `question_bank_question` (
    `id`              BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `questionBankId`  BIGINT  NOT NULL COMMENT '题库 id',
     `questionId`      BIGINT  NOT NULL COMMENT '题目 id',
     `questionOrder`   INT     DEFAULT 0 NOT NULL COMMENT '题目顺序（题号）',
     `userId`          BIGINT  NOT NULL COMMENT '创建用户 id',
    `createTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE (questionBankId, questionId)
    ) COMMENT '题库题目' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 评分结果表（新增 isDynamic 字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `scoring_result` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `resultName`       VARCHAR(128) NOT NULL,
    `resultDesc`       TEXT NULL,
    `isDynamic`        TINYINT      DEFAULT 0 COMMENT '是否动态生成: 0-预设, 1-AI生成',
    `questionbankId`   BIGINT NOT NULL COMMENT '关联题单ID',
    `userId`           BIGINT NOT NULL COMMENT '创建人ID',
    `score` INT NULL COMMENT '得分',
    `duration` INT NULL COMMENT '答题用时（秒）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '答题状态（0-未完成，1-已完成）',
    `createTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT NOT NULL DEFAULT 0,
    INDEX `idx_questionbankId` (`questionbankId`)
    ) COMMENT '评分结果表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 用户答题记录表（优化索引）
-- ----------------------------
USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `user_answer` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `questionbankId`   BIGINT NOT NULL COMMENT '题单ID',
    `questionbankType` TINYINT  NOT NULL DEFAULT 0 COMMENT '题单类型',
    `scoringStrategy`  TINYINT  NOT NULL DEFAULT 0 COMMENT '评分策略',
    `choices`          TEXT NULL COMMENT '用户答案JSON',
    `resultId`         BIGINT NULL COMMENT '评分结果ID',
    `resultName`       VARCHAR(128) NULL,
    `resultDesc`       TEXT NULL,
    `resultPicture`   VARCHAR(1024) NULL,
    `resultScore`      INT NULL COMMENT '得分',
    `userAnswerId`           BIGINT NOT NULL COMMENT '学生ID',
    `createTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT NOT NULL DEFAULT 0,
    INDEX `idx_questionbankId` (`questionbankId`),
    INDEX `idx_userId` (`userAnswerId`)
    ) COMMENT '用户答题记录表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
USE `xuesisi`;
CREATE TABLE `learning_analysis` (
        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
        `user_id` bigint NOT NULL COMMENT '学生ID',
        `class_id` bigint NOT NULL COMMENT '班级ID',
        `total_score` int NOT NULL COMMENT '累计总分',
        `weak_tags` json DEFAULT NULL COMMENT '薄弱知识点ID集合（JSON数组）',
        `knowledge_point_stats` TEXT COMMENT '知识点统计（更详细的知识点分析）',
        `tag_stats` json DEFAULT NULL COMMENT '标签统计（如{"编程": {"correct": 5, "total": 10}}）',
        `question_bank_id` bigint NOT NULL COMMENT '题库ID',
        `question_id` json NOT NULL COMMENT '题目ID（JSON数组）',
        `user_answer` json NOT NULL COMMENT '用户答案',
        `analysis` text COMMENT '分析内容',
        `suggestion` text COMMENT '改进建议',
        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
        PRIMARY KEY (`id`),
        KEY `idx_user_id` (`user_id`),
        KEY `idx_class_id` (`class_id`),
        KEY `idx_question_bank_id` (`question_bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习分析统计表';


-- ----------------------------
-- 知识点表
-- ----------------------------
USE `xuesisi`;
CREATE TABLE IF NOT EXISTS `knowledge_point` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '知识点ID' PRIMARY KEY,
    `name`             VARCHAR(128)  NOT NULL COMMENT '知识点名称',
    `description`      TEXT          NULL COMMENT '知识点描述',
    `subject`          VARCHAR(64)   NOT NULL COMMENT '学科',
    `userId`           BIGINT        NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT       NOT NULL DEFAULT 0,
    INDEX `idx_userId` (`userId`),
    INDEX `idx_subject` (`subject`)
    ) COMMENT '知识点表' COLLATE = utf8mb4_unicode_ci;
-- ----------------------------
-- 题目-知识点关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question_knowledge` (
    `id`             BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `questionId`     BIGINT NOT NULL COMMENT '题目ID',
    `knowledgeId`    BIGINT NOT NULL COMMENT '知识点ID',
    `createTime`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uniq_question_knowledge` (`questionId`, `knowledgeId`),
    INDEX `idx_knowledgeId` (`knowledgeId`)
) COMMENT '题目-知识点关系表' COLLATE = utf8mb4_unicode_ci;
USE `xuesisi`;


CREATE TABLE IF NOT EXISTS `teaching_plan` (
       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
       `question_bank_id` bigint NOT NULL COMMENT '题库ID',
       `user_id` bigint NOT NULL COMMENT '用户ID',
       `user_answer_id` bigint NOT NULL COMMENT '用户答题ID',
       `knowledge_analysis` json COMMENT '知识点分析（JSON格式）',
       `teaching_objectives` json COMMENT '教学目标（JSON格式）',
       `teaching_arrangement` json COMMENT '教学安排（JSON格式）',
       `expected_outcomes` json COMMENT '预期学习成果（JSON格式）',
       `evaluation_methods` json COMMENT '评估方法（JSON格式）',
        `subject` VARCHAR(50) COMMENT '学科',
       `title` VARCHAR(50) COMMENT '教案标题',
       `knowledge_points` JSON COMMENT '知识点列表',
       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
       `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
       PRIMARY KEY (`id`),
       KEY `idx_user_answer_id` (`user_answer_id`),
       KEY `idx_user_id` (`user_id`),
       KEY `idx_question_bank_id` (`question_bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学教案表';
USE `xuesisi`;

