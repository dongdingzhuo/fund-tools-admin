-- 创建用户信息表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` bigint NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `nick_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_1` (`user_name`)
);

-- 插入测试用户数据
INSERT INTO `t_user` (`id`, `user_name`, `nick_name`) VALUES
(1, 'dongdingzhuo', '董定卓'),
(2, 'admin', '管理员');
