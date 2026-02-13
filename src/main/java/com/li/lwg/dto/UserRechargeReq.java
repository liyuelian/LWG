package com.li.lwg.dto;

/**
 * 充值请求参数
 */
public class UserRechargeReq {
    private Long userId;

    private Long amount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}