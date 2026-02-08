package com.li.lwg.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 资金流水实体
 */
@Data
public class TransactionLog {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 关联任务ID (可选)
     */
    private Long missionId;

    /**
     * 变动金额 (正数增加, 负数减少)
     */
    private Long amount;

    /**
     * 交易后余额 (快照) - 核心字段
     */
    private Long balanceAfter;

    /**
     * 类型: 1-充值, 2-发布冻结, 3-完成结算, 4-取消退回, 5-提现
     */
    private Integer type;

    /**
     * 外部订单号 (唯一)
     */
    private String orderNo;

    /**
     * 流水备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}