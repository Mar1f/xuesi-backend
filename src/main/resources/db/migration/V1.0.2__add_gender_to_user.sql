ALTER TABLE `user`
ADD COLUMN `gender` tinyint(1) DEFAULT NULL COMMENT '性别 0-女 1-男' AFTER `user_role`; 