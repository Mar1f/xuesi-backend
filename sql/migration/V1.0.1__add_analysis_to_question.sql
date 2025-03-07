-- 添加题目解析字段
ALTER TABLE `question` ooiioioioiiioioioionkvjhccgch















    








ADD COLUMN `analysis` TEXT NULL COMMENT '题目解析' 
AFTER `source`;

-- 回滚语句
-- ALTER TABLE `question` DROP COLUMN `analysis`; 