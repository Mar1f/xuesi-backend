CREATE TABLE IF NOT EXISTS `learning_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `questionBankId` bigint NOT NULL COMMENT '题库ID',
  `questionId` bigint DEFAULT NULL COMMENT '题目ID',
  `userAnswer` text COMMENT '用户答案',
  `score` int DEFAULT NULL COMMENT '得分',
  `analysis` text COMMENT '分析内容',
  `suggestion` text COMMENT '改进建议',
  `isOverall` tinyint(1) DEFAULT '0' COMMENT '是否为总体评价',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_questionBankId` (`questionBankId`),
  KEY `idx_questionId` (`questionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习分析表'; 