package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
