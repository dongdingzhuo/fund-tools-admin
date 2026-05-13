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

-- 创建自选基金表
CREATE TABLE IF NOT EXISTS `t_fund_self` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name` varchar(255) NOT NULL COMMENT '用户账号',
  `fund_code` varchar(20) NOT NULL COMMENT '基金代码',
  `base_amount` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '成本价',
  `share_quantity` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '持有份额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_fund` (`user_name`, `fund_code`),
  KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自选基金表';

-- 创建实时基金数据表
CREATE TABLE IF NOT EXISTS `t_fund_last` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(20) NOT NULL COMMENT '基金代码',
  `fund_name` varchar(255) NOT NULL COMMENT '基金名称',
  `profit_percent` decimal(10, 4) DEFAULT '0.0000' COMMENT '最新收益率',
  `current_price` decimal(10, 4) DEFAULT '0.0000' COMMENT '净值',
  `prev_price` decimal(10, 4) DEFAULT '0.0000' COMMENT '上一个交易日净值',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_code` (`fund_code`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时基金数据表';

-- 创建节假日日期表
CREATE TABLE IF NOT EXISTS `t_holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `date` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd格式）',
  `workday_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '工作日标识（Y/N）',
  `holiday_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '节假日标识（Y/N）',
  `weekend_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '周末标识（Y/N）',
  `trading_day_flag` char(1) NOT NULL DEFAULT 'N' COMMENT '交易日标识（Y/N）',
  `week` tinyint NOT NULL COMMENT '星期（0-周日，1-周一，2-周二，3-周三，4-周四，5-周五，6-周六）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date` (`date`),
  KEY `idx_workday_flag` (`workday_flag`),
  KEY `idx_trading_day_flag` (`trading_day_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日日期表';
