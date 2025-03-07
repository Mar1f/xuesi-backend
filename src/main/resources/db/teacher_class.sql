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