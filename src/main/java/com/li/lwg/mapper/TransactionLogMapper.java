package com.li.lwg.mapper;

import com.li.lwg.entity.TransactionLog;
import com.li.lwg.vo.FinanceOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 查询用户【累计收支】和【本月收支】
     *
     * @param userId       用户ID
     * @param monthStart   本月开始时间
     * @param incomeTypes  收入类型列表(来自枚举)
     * @param expenseTypes 支出类型列表(来自枚举)
     * @return 财务概览视图对象
     */
    FinanceOverviewVO selectUserFinanceOverview(
            @Param("userId") Long userId,
            @Param("monthStart") String monthStart,
            @Param("incomeTypes") List<Integer> incomeTypes,
            @Param("expenseTypes") List<Integer> expenseTypes
    );

}
