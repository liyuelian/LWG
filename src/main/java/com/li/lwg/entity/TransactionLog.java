package com.li.lwg.entity;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 资金流水实体
 */
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
     * 资产类型
     * 1: 可用余额 (Available)
     * 2: 冻结余额 (Frozen)
     */
    private Integer assetType;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getAssetType() {
        return assetType;
    }

    public void setAssetType(Integer assetType) {
        this.assetType = assetType;
    }
}