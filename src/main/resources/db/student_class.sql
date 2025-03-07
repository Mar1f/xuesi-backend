CREATE TABLE IF NOT EXISTS `student_class` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `studentId` bigint NOT NULL COMMENT '学生ID',
    `classId` bigint NOT NULL COMMENT '班级ID',
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_class` (`studentId`, `classId`),
    KEY `idx_studentId` (`studentId`),
    KEY `idx_classId` (`classId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生-班级关联表'; 