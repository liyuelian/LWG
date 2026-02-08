package com.li.lwg.mapper;

import com.li.lwg.entity.TransactionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Mapper
public interface TransactionLogMapper {
    int insert(TransactionLog log);
}
