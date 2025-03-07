CREATE TABLE IF NOT EXISTS `class` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `className` varchar(50) NOT NULL COMMENT '班级名称',
    `teacherId` bigint NOT NULL COMMENT '班主任ID',
    `description` varchar(500) DEFAULT NULL COMMENT '班级描述',
    `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_teacherId` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表'; 