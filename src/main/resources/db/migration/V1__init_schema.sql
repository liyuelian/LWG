-- ============================================================================
-- LWG 灵务阁 · 初始表结构
--
-- 来源：从开发库 lwg 导出的真实 DDL（mysqldump --no-data），已去除
--       会话级 SET 指令与 AUTO_INCREMENT 计数器值，保证每次在全新库上
--       执行的结果完全一致。
--
-- 与开发库的两处「有意偏差」（仅注释文案，不影响结构）：
--   1. t_transaction_log.type 的第 4 类改写为「悬赏退回」，与代码枚举
--      TransactionType.REFUND(4, "悬赏退回") 保持一致；开发库残留的是
--      早期文案「任务取消/驳回退款」。
--   2. t_reputation_log.source_type 改写为「1-任务结算, 2-任务超时」，与
--      ReputationSourceEnum 保持一致；开发库残留的是「2-违约扣除, 3-人工调整」，
--      与枚举和消费者 ReputationListener 的实际行为都不符。
--
-- 约定：
--   1. 全部使用 CREATE TABLE IF NOT EXISTS，可安全重放；
--   2. 不写 AUTO_INCREMENT=0，由 MySQL 自行初始化；
--   3. 排序按依赖与业务主线：用户 -> 任务 -> 资金流水 -> 信誉流水；
--   4. 字符集统一 utf8mb4，与 MySQL 8.0+ 的默认排序规则一致；
--   5. 列不写冗余的 COLLATE，继承表级默认，与 SHOW CREATE TABLE 的
--      规范化输出保持逐字节一致。
--
-- 维护：后续任何表结构变更都必须新增 V2__xxx.sql、V3__xxx.sql，
--       严禁修改本文件（已执行过的迁移改动会导致 checksum 校验失败）。
-- ============================================================================


-- 修士信息表：账号、境界、双钱包（可用/冻结）、信誉值
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '道号',
  `password` varchar(128) NOT NULL COMMENT '密码(加密)',
  `realm` tinyint NOT NULL DEFAULT '1' COMMENT '境界:1-炼气,2-筑基,3-金丹,4-元婴,5-化神,6-炼虚,7-合体,8-大乘,9-渡劫',
  `balance` bigint NOT NULL DEFAULT '0' COMMENT '可用灵石(单位:分)',
  `frozen_balance` bigint NOT NULL DEFAULT '0' COMMENT '冻结灵石(单位:分)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0-禁用,1-正常',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reputation` int DEFAULT '6000' COMMENT '信誉值: 初始6000, 上限12000',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='修士信息表';


-- 悬赏任务表：状态机 0-待接单 1-进行中 2-待验收 3-已完成 4-已取消
CREATE TABLE IF NOT EXISTS `t_mission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务详情',
  `mission_type` tinyint DEFAULT '1' COMMENT '类型:1-降妖,2-采集,3-护送,4-其他',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度:1-简单,2-普通,3-困难,4-地狱',
  `min_realm` tinyint DEFAULT '1' COMMENT '最低境界要求(对应用户realm)',
  `reward` bigint NOT NULL COMMENT '悬赏金额(单位:分)',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `acceptor_id` bigint DEFAULT NULL COMMENT '接单者ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0-待接,1-进行,2-待验,3-完成,4-已取消',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `proof_data` json DEFAULT NULL COMMENT '任务凭证(JSON格式)',
  `deadline` datetime DEFAULT NULL COMMENT '任务截止时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁(防超卖)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `accept_time` datetime DEFAULT NULL COMMENT '接取时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_acceptor` (`acceptor_id`),
  KEY `idx_type_status` (`mission_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='悬赏任务表';


-- 资金流水表：复式记账，asset_type 区分可用/冻结钱包，order_no 关联同一笔业务的两条分录
CREATE TABLE IF NOT EXISTS `t_transaction_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联用户',
  `mission_id` bigint DEFAULT NULL COMMENT '关联任务',
  `amount` bigint NOT NULL COMMENT '变动金额(正数增加,负数减少)',
  `balance_after` bigint NOT NULL COMMENT '交易后余额(快照)',
  `type` tinyint NOT NULL COMMENT '动账类型: 1-发布悬赏, 2-结算支出, 3-任务收益, 4-悬赏退回, 5-灵石充值',
  `asset_type` tinyint NOT NULL DEFAULT '1' COMMENT '资产类型: 1-可用余额, 2-冻结余额',
  `order_no` varchar(64) DEFAULT NULL COMMENT '外部订单号',
  `remark` varchar(255) DEFAULT NULL COMMENT '流水备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_mission` (`mission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金流水表';


-- 信誉变更流水表：source_type + source_id 用于 MQ 消费幂等
CREATE TABLE IF NOT EXISTS `t_reputation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `change_score` int NOT NULL COMMENT '变动值',
  `current_score` int NOT NULL COMMENT '变动后的当前值',
  `source_type` tinyint NOT NULL COMMENT '来源: 1-任务结算, 2-任务超时',
  `source_id` bigint DEFAULT NULL COMMENT '关联ID (任务ID)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信誉变更流水表';
