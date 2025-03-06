-- 添加题目数量字段
ALTER TABLE `question_bank` 
ADD COLUMN `questionCount` INT NOT NULL DEFAULT 0 COMMENT '题目数量' 
AFTER `isDelete`;

-- 回滚语句
-- ALTER TABLE `question_bank` DROP COLUMN `questionCount`; 