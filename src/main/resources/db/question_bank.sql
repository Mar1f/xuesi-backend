CREATE TABLE IF NOT EXISTS `question_bank` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(128) NOT NULL COMMENT '题库名称',
    `description` text COMMENT '题库描述',
    `questionCount` int DEFAULT '0' COMMENT '题目数量',
    `classId` bigint NOT NULL COMMENT '所属班级ID',
    `teacherId` bigint NOT NULL COMMENT '创建教师ID',
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_classId` (`classId`),
    KEY `idx_teacherId` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库表'; 