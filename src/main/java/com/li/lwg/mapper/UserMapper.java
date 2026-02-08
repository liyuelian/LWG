package com.li.lwg.mapper;

import com.li.lwg.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Mapper
public interface UserMapper {

    /**
     * 发布任务时扣除资金接口，利用数据库行锁防止并发超扣
     *
     * @param userId 用户id
     * @param amount 金额
     * @return 1=扣款成功，0=余额不足
     */
    int freezeBalance(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 查询用户信息
     *
     * @param id 用户id
     * @return 用户信息
     */
    User selectById(@Param("id") Long id);

}
