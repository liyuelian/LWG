package com.li.lwg.mapper;

import com.li.lwg.entity.TransactionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Mapper
public interface TransactionLogMapper {
    int insert(TransactionLog log);

    /**
     * 查询用户流水列表
     *
     * @param userId 用户Id
     * @return 流水列表
     */
    List<TransactionLog> selectByUserId(Long userId);
}
