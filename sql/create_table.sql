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
     `id`          BIGINT AUTO_INCREMENT COMMENT '班级ID' PRIMARY KEY,
     `className`   VARCHAR(128) NOT NULL COMMENT '班级名称',
    `teacherId`   BIGINT       NOT NULL COMMENT '教师ID（关联user.id）',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`    TINYINT      NOT NULL DEFAULT 0,
    INDEX `idx_teacherId` (`teacherId`)
    ) COMMENT '班级表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 学生班级关系表（
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_class` (
      `id`         BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
      `userId`     BIGINT   NOT NULL COMMENT '学生ID',
      `classId`    BIGINT   NOT NULL COMMENT '班级ID',
      `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (userId, classId)
    ) COMMENT '学生班级关系表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question_bank` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '题单ID' PRIMARY KEY,
    `title`            VARCHAR(128)  NOT NULL COMMENT '题单名称',
    `description`      TEXT          NULL COMMENT '描述',
    `picture`          VARCHAR(1024) NULL COMMENT '图标',
    `questionBankType` TINYINT       NOT NULL DEFAULT 0 COMMENT '类型: 0-得分类, 1-测评类',
    `scoringStrategy`  TINYINT       NOT NULL DEFAULT 0 COMMENT '评分策略: 0-自定义, 1-AI',
    `totalScore`       INT           NOT NULL DEFAULT 100 COMMENT '题单总分',
    `passScore`        INT           NOT NULL DEFAULT 60 COMMENT '及格分',
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
CREATE TABLE IF NOT EXISTS `question` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '题目ID' PRIMARY KEY,
    `questionContent`  TEXT           NOT NULL COMMENT '题干文本',
    `tags`             VARCHAR(1024)  NULL COMMENT '标签列表（json 数组）',
    `questionType`     TINYINT        NOT NULL COMMENT '题型: 0-单选, 1-多选, 2-填空',
    `options`          TEXT           NULL COMMENT '选项（JSON数组, 如["A","B"]）',
    `answer`           VARCHAR(512)   NOT NULL COMMENT '正确答案',
    `score`            INT            NOT NULL DEFAULT 10 COMMENT '题目分值',
    `source`           TINYINT        NOT NULL DEFAULT 0 COMMENT '来源: 0-手动, 1-AI生成',
    `userId`           BIGINT         NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT        NOT NULL DEFAULT 0,
    INDEX `idx_userId` (`userId`)
    ) COMMENT '题目表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题库题目表（保持不变）
-- ----------------------------
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
    `resultScoreRange` VARCHAR(64)  NULL COMMENT '得分范围表达式（如">=80"）',
    `isDynamic`        TINYINT      DEFAULT 0 COMMENT '是否动态生成: 0-预设, 1-AI生成',
    `questionbankId`   BIGINT NOT NULL COMMENT '关联题单ID',
    `userId`           BIGINT NOT NULL COMMENT '创建人ID',
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
-- 用户答题详情表（新增表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_answer_detail` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `userId`  BIGINT NOT NULL COMMENT '关联user_answer.id',
    `questionId`    BIGINT NOT NULL COMMENT '题目ID',
    `userChoice`    TEXT COMMENT '用户答案',
     `isCorrect`     TINYINT COMMENT '是否正确: 0-否, 1-是',
    `score`         INT COMMENT '本题得分',
    `createTime`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_userId` (`userId`)
    ) COMMENT '用户答题详情表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 学习分析统计表（新增 tagStats 字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `learning_analysis` (
    `userId`       BIGINT NOT NULL COMMENT '学生ID',
    `classId`      BIGINT NOT NULL COMMENT '班级ID',
     `totalScore`   INT COMMENT '累计总分',
     `avgScore`     DECIMAL(5,2) COMMENT '平均分',
    `weakTags`     TEXT COMMENT '薄弱知识点ID集合（JSON数组）',
    `tagStats`     TEXT COMMENT '标签统计（如{"编程": {"correct": 5, "total": 10}}）',
    `createTime`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`userId`, `classId`),
    INDEX `idx_classId` (`classId`)
    ) COMMENT '学习分析统计表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 知识点表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_point` (
    `id`          BIGINT AUTO_INCREMENT COMMENT '知识点ID' PRIMARY KEY,
     `name`        VARCHAR(128) NOT NULL COMMENT '知识点名称',
    `description` TEXT NULL COMMENT '知识点描述',
    `userId`      BIGINT NOT NULL COMMENT '创建人ID（教师或管理员）',
    `createTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uniq_name` (`name`)
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