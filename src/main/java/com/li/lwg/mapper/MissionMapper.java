package com.li.lwg.mapper;

import com.li.lwg.entity.Mission;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Mapper
public interface MissionMapper {

    /**
     * 插入任务
     * @param mission 任务信息
     * @return 反馈
     */
    int insert(Mission mission);
}
