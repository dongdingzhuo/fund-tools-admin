USE fund;
-- 创建用户信息表
CREATE TABLE `t_user` (
  `id` bigint NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `nick_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`) ,
  UNIQUE KEY `idx_1` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';
