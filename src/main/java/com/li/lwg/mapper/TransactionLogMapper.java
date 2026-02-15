package com.li.lwg.mapper;

import com.li.lwg.dto.TransactionPageReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.vo.FinanceOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    /**
     * 流水明细 - 列表查询
     */
    List<TransactionLog> selectTransactionPage(TransactionPageReq req);

    /**
     * 流水明细 - 总数查询
     */
    Long countTransaction(TransactionPageReq req);

    /**
     * 查趋势：按月分组统计 (近12个月)用户的收支情况
     *
     * @param userId       用户ID
     * @param startTime    开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param incomeTypes  动态收入ID列表
     * @param expenseTypes 动态支出ID列表
     */
    List<Map<String, Object>> selectYearlyTrend(
            @Param("userId") Long userId,
            @Param("startTime") String startTime,
            @Param("incomeTypes") List<Integer> incomeTypes,
            @Param("expenseTypes") List<Integer> expenseTypes
    );

    /**
     * 查分布：按类型分组统计用户收支
     *
     * @param userId   用户ID
     * @param typeList 需要统计的所有类型ID (收入+支出)
     */
    List<Map<String, Object>> selectTypeDistribution(
            @Param("userId") Long userId,
            @Param("typeList") List<Integer> typeList
    );

}
