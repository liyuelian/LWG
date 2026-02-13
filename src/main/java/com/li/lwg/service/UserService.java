package com.li.lwg.service;

import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;

import java.util.List;

public interface UserService {

    /**
     * 查询用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    User getUserInfo(Long userId);

    /**
     * 我的交易流水查询
     * @param userId 用户id
     * @return 流水列表
     */
    List<TransactionLog> getMyTransactions(Long userId);

    /**
     * 用户充值接口
     *
     * @param req
     */
    void recharge(UserRechargeReq req);
}