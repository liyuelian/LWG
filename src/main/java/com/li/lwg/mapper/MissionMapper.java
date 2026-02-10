package com.li.lwg.mapper;

import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.entity.Mission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 查询任务
     * @return
     */
    List<Mission> selectList(MissionQueryReq req);

    /**
     * 根据ID查任务
     *
     * @param id 任务号
     */
    Mission selectById(Long id);

    /**
     * 抢单 (乐观锁更新)
     *
     * @param id         任务ID
     * @param acceptorId 接单人ID
     * @param version    当前查到的版本号
     * @return 影响行数 (1=成功, 0=失败/版本号变了)
     */
    int acceptMission(@Param("id") Long id,
                      @Param("acceptorId") Long acceptorId,
                      @Param("version") Integer version);

    /**
     * 提交任务
     *
     * @param id        任务ID
     * @param userId    提交人ID (用于校验)
     * @param proofData 凭证
     * @return 影响行数
     */
    int submitMission(@Param("id") Long id,
                      @Param("userId") Long userId,
                      @Param("proofData") String proofData);

    /**
     * 更新任务状态
     *
     * @param id     任务ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
