package com.li.lwg.mapper;

import com.li.lwg.entity.ReputationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReputationLogMapper {

    /**
     * 插入信誉流水记录
     *
     * @param record 流水实体
     * @return 影响行数
     */
    int insert(ReputationLog record);

    /**
     * 条件筛选查询记录数
     *
     * @param sourceId   来源ID
     * @param sourceType 来源类型
     * @return 记录条数
     */
    int countBySource(@Param("sourceId") Long sourceId, @Param("sourceType") Integer sourceType);
}