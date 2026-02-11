package com.li.lwg.service;

import com.li.lwg.entity.User;

public interface UserService {

    /**
     * 查询用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    User getUserInfo(Long userId);
}