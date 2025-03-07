-- 添加题目数量字段
USE `xuesisi`;
ALTER TABLE `user`
    ADD COLUMN `gender` tinyint(1) DEFAULT NULL COMMENT '性别 0-女 1-男' AFTER `isDelete`;
-- 回滚语句
-- ALTER TABLE `question_bank` DROP COLUMN `questionCount`; 