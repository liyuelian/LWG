package com.li.lwg.service.impl;

import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.mapper.TransactionLogMapper;
import com.li.lwg.mapper.UserMapper;
import com.li.lwg.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TransactionLogMapper transactionLogMapper;

    @Override
    public List<TransactionLog> getMyTransactions(Long userId) {
        return transactionLogMapper.selectByUserId(userId);
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}