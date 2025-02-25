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
-- 学生班级关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_class` (
                                            `userId`      BIGINT   NOT NULL COMMENT '学生ID',
                                            `classId`     BIGINT   NOT NULL COMMENT '班级ID',
                                            `createTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (`userId`, `classId`),
    INDEX `idx_classId` (`classId`)
    ) COMMENT '学生班级关系表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `assignment` (
                                            `id`               BIGINT AUTO_INCREMENT COMMENT '题单ID' PRIMARY KEY,
                                            `assignmentName`   VARCHAR(128)  NOT NULL COMMENT '题单名称',
    `assignmentDesc`   VARCHAR(2048) NULL COMMENT '题单描述',
    `classId`          BIGINT        NULL COMMENT '所属班级ID（null表示公开）',
    `appIcon`          VARCHAR(1024) NULL COMMENT '图标',
    `appType`          TINYINT       NOT NULL DEFAULT 0 COMMENT '类型: 0-得分类, 1-测评类',
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
    INDEX `idx_classId` (`classId`),
    INDEX `idx_userId` (`userId`)
    ) COMMENT '题单表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题目表（
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question` (
    `id`               BIGINT AUTO_INCREMENT COMMENT '题目ID' PRIMARY KEY,
    `assignmentId`     BIGINT        NOT NULL COMMENT '所属题单ID',
    `questionContent`  TEXT          NOT NULL COMMENT '题干文本',
    `questionType`     TINYINT       NOT NULL COMMENT '题型: 0-单选, 1-多选, 2-填空',
    `options`          TEXT          NULL COMMENT '选项（JSON数组, 如["A","B"]）',
    `correctAnswer`    VARCHAR(512)  NOT NULL COMMENT '正确答案',
    `score`            INT           NOT NULL DEFAULT 10 COMMENT '题目分值',
    `userId`           BIGINT        NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT       NOT NULL DEFAULT 0,
    INDEX `idx_assignmentId` (`assignmentId`)
    ) COMMENT '题目表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 知识点标签表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tagName`     VARCHAR(64) NOT NULL COMMENT '知识点名称',
    `createTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_tagName` (`tagName`)
    ) COMMENT '知识点标签表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 题目标签关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question_tag` (
    `questionId`  BIGINT NOT NULL,
    `tagId`       BIGINT NOT NULL,
    PRIMARY KEY (`questionId`, `tagId`)
    ) COMMENT '题目-标签关联表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 评分结果表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `scoring_result` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `resultName`       VARCHAR(128) NOT NULL,
    `resultDesc`       TEXT NULL,
    `resultPicture`    VARCHAR(1024) NULL,
    `resultProp`       VARCHAR(128) NULL COMMENT '属性集合JSON',
    `resultScoreRange` INT NULL COMMENT '得分范围（如>=80）',
    `assignmentId`     BIGINT NOT NULL COMMENT '关联题单ID',
    `userId`           BIGINT NOT NULL COMMENT '创建人ID',
    `createTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`         TINYINT NOT NULL DEFAULT 0,
    INDEX `idx_assignmentId` (`assignmentId`),
    INDEX `idx_score_range` (`resultScoreRange`, `assignmentId`)
    ) COMMENT '评分结果表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 用户答题记录表（优化索引）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_answer` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `assignmentId`    BIGINT NOT NULL COMMENT '题单ID',
    `appType`         TINYINT  NOT NULL DEFAULT 0 COMMENT '题单类型',
    `scoringStrategy` TINYINT  NOT NULL DEFAULT 0 COMMENT '评分策略',
    `choices`         TEXT NULL COMMENT '用户答案JSON',
    `resultId`        BIGINT NULL COMMENT '评分结果ID',
    `resultName`      VARCHAR(128) NULL,
    `resultDesc`      TEXT NULL,
    `resultPicture`   VARCHAR(1024) NULL,
    `resultScore`     INT NULL COMMENT '得分',
    `userId`          BIGINT NOT NULL COMMENT '学生ID',
    `isArchive`       TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档',
    `createTime`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete`        TINYINT NOT NULL DEFAULT 0,
    INDEX `idx_user_assignment` (`userId`, `assignmentId`),
    INDEX `idx_assignmentId` (`assignmentId`)
    ) COMMENT '用户答题记录表' COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 学习分析统计表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `learning_analysis` (
    `userId`      BIGINT NOT NULL COMMENT '学生ID',
    `classId`     BIGINT NOT NULL COMMENT '班级ID',
    `totalScore`  INT COMMENT '累计总分',
    `avgScore`    DECIMAL(5,2) COMMENT '平均分',
    `weakTags`    TEXT COMMENT '薄弱知识点ID集合（JSON数组）',
    `createTime`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`userId`, `classId`),
    INDEX `idx_classId` (`classId`)
    ) COMMENT '学习分析统计表' COLLATE = utf8mb4_unicode_ci;