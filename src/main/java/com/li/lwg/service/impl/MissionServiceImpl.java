package com.li.lwg.service.impl;

import com.li.lwg.dto.MissionAcceptReq;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.entity.Mission;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.exception.ServiceException;
import com.li.lwg.mapper.MissionMapper;
import com.li.lwg.mapper.TransactionLogMapper;
import com.li.lwg.mapper.UserMapper;
import com.li.lwg.service.MissionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Service
public class MissionServiceImpl implements MissionService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private MissionMapper missionMapper;
    @Resource
    private TransactionLogMapper transactionLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishMission(MissionPublishReq req) {
        // 1. 基础校验
        if (req.getReward() <= 0) {
            throw new ServiceException("悬赏金额必须大于0");
        }

        // 2. 扣减余额 (原子更新)
        int rows = userMapper.freezeBalance(req.getPublisherId(), req.getReward());
        if (rows == 0) {
            // 【关键】抛出业务异常，GlobalExceptionHandler 会捕获它并返回 400 给前端
            // 同时 @Transactional 会自动回滚事务
            throw new ServiceException("发布失败：您的灵石不足");
        }

        // 3. 插入任务
        Mission mission = new Mission();
        BeanUtils.copyProperties(req, mission);
        mission.setStatus(0); // 待接单
        mission.setVersion(0);
        missionMapper.insert(mission);

        // 4. 记录流水
        User user = userMapper.selectById(req.getPublisherId());

        TransactionLog log = new TransactionLog();
        log.setUserId(req.getPublisherId());
        log.setMissionId(mission.getId());
        log.setAmount(-req.getReward());
        log.setBalanceAfter(user.getBalance());
        log.setType(2); // 发布冻结
        log.setOrderNo(UUID.randomUUID().toString());
        log.setRemark("发布悬赏：" + req.getTitle());

        transactionLogMapper.insert(log);
        return mission.getId();

    }

    @Override
    public List<Mission> getMissionList(MissionQueryReq req) {
        if (req.getStatus() == null) {
            //待接单(0)
            req.setStatus(0);
        }
        return missionMapper.selectList(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptMission(MissionAcceptReq req) {
        // 1. 先查询任务状态 (这一步拿到 version)
        Mission mission = missionMapper.selectById(req.getMissionId());

        // 2. 基础校验 (Fail Fast)
        if (mission == null) {
            throw new ServiceException("任务不存在");
        }
        if (mission.getStatus() != 0) {
            throw new ServiceException("手慢了，任务已被抢走或取消");
        }
        if (mission.getPublisherId().equals(req.getAcceptorId())) {
            throw new ServiceException("道友，不能接自己发布的悬赏刷单哦");
        }

        User acceptor = userMapper.selectById(req.getAcceptorId());
        if (acceptor == null) {
            throw new ServiceException("抢单失败：接单弟子不存在（ID无效）");
        }

        // 3. 执行乐观锁更新 (CAS: Compare And Swap)
        // 传入刚才查出来的 version。如果数据库里 version 变了，这里会返回 0
        int rows = missionMapper.acceptMission(
                req.getMissionId(),
                req.getAcceptorId(),
                mission.getVersion()
        );

        if (rows == 0) {
            // 返回 0 说明你在 select 和 update 之间，有人抢先一步改了数据
            throw new ServiceException("抢单失败，该任务十分抢手，请刷新重试");
        }

        return true;
    }
}
