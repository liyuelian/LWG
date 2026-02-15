package com.li.lwg.service;

import com.li.lwg.common.PageResult;
import com.li.lwg.dto.TransactionPageReq;
import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.vo.FinanceChartVO;
import com.li.lwg.vo.FinanceOverviewVO;

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
     *
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

    /**
     * 查询用户【累计收支】和【本月收支】
     *
     * @param userId 用户ID
     * @return 累积、本月收支
     */
    FinanceOverviewVO getFinanceOverview(Long userId);

    /**
     * 筛选查询用户流水
     */
    PageResult<TransactionLog> getTransactionPage(TransactionPageReq req);

    /**
     * 查询用户近12个月收支情况
     * @param userId 用户ID
     * @return 折线图数据、饼图数据
     */
    FinanceChartVO getFinanceCharts(Long userId);
}