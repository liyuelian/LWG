package com.li.lwg.controller;

import com.li.lwg.common.PageResult;
import com.li.lwg.common.Result;
import com.li.lwg.dto.TransactionPageReq;
import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.service.UserService;
import com.li.lwg.vo.FinanceOverviewVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author liyuelian
 * @date 2026/2/11
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;


    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestParam Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @GetMapping("/transactions")
    public Result<List<TransactionLog>> getMyTransactions(@RequestParam Long userId) {
        return Result.success(userService.getMyTransactions(userId));
    }

    /**
     * 充值接口
     */
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestBody @Validated UserRechargeReq req) {
        userService.recharge(req);
        return Result.success("充值成功，灵石已到账！");
    }

    /**
     * 查询用户【累计收支】和【本月收支】
     */
    @GetMapping("/finance/overview")
    public Result<FinanceOverviewVO> getFinanceOverview(@RequestParam Long userId) {
        return Result.success(userService.getFinanceOverview(userId));
    }

    /**
     * 条件筛选用户流水
     */
    @PostMapping("/transaction/list")
    public Result<PageResult<TransactionLog>> getTransactionList(@RequestBody TransactionPageReq req) {
        return Result.success(userService.getTransactionPage(req));
    }
}
