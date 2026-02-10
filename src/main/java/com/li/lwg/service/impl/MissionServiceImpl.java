package com.li.lwg.service.impl;

import com.li.lwg.common.TransactionType;
import com.li.lwg.dto.*;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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
    @Resource
    private ObjectMapper objectMapper;

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
        log.setType(TransactionType.PUBLISH.getCode());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitMission(MissionSubmitReq req) {
        // 1. 简单校验
        if (req.getProofData() == null) {
            throw new ServiceException("交付凭证不能为空，请上传妖丹或任务记录");
        }

        String proofJsonStr = "{}";
        try {
            proofJsonStr = objectMapper.writeValueAsString(req.getProofData());
        } catch (Exception e) {
            throw new ServiceException("凭证格式错误，无法解析");
        }

        // 2. 执行更新 (Mapper里已经限制了必须是本人且状态为1)
        int rows = missionMapper.submitMission(
                req.getMissionId(),
                req.getUserId(),
                proofJsonStr
        );

        if (rows == 0) {
            // 如果更新失败，可能是任务ID不对，或者不是这人接的，或者状态不是进行中
            // 为了给用户更精确的提示，可以先查一下 (可选优化)
            Mission mission = missionMapper.selectById(req.getMissionId());
            if (mission == null) {
                throw new ServiceException("任务不存在");
            }
            if (!mission.getAcceptorId().equals(req.getUserId())) {
                throw new ServiceException("这不是你接的任务，无法交付");
            }
            if (mission.getStatus() != 1) {
                throw new ServiceException("任务状态异常，无法交付");
            }
            throw new ServiceException("提交失败，请重试");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditMission(MissionAuditReq req) {
        // 1. 查询任务详情
        Mission mission = missionMapper.selectById(req.getMissionId());

        // 2. 基础风控校验 (Fail Fast)
        if (mission == null) {
            throw new ServiceException("任务不存在");
        }
        // 校验身份：只有发布者才有资格审核
        if (!mission.getPublisherId().equals(req.getUserId())) {
            throw new ServiceException("无权审核他人发布的任务");
        }
        // 校验状态：只有“待结算(2)”状态的任务才能审核
        if (mission.getStatus() != 2) {
            throw new ServiceException("任务状态异常，无法结算（可能已结算或未提交）");
        }

        // 3. 核心业务分支
        if (req.getPass()) {
            // === 分支 A: 审核通过 (发钱！) ===

            // 3.1 扣减发布者冻结金额 (支出)
            // 这一步如果返回 0，说明发布者冻结余额不对劲（比如被系统扣过费了），必须回滚
            int rows1 = userMapper.decreaseFrozen(mission.getPublisherId(), mission.getReward());
            if (rows1 == 0) {
                throw new ServiceException("结算失败：发布者冻结资金异常");
            }

            // 3.2 扣完钱后，查询发布者的账户状态
            User publisher = userMapper.selectById(mission.getPublisherId());

            // 3.3 记录流水：发布者支出
            TransactionLog logPub = new TransactionLog();
            logPub.setUserId(mission.getPublisherId());
            logPub.setType(TransactionType.SETTLEMENT.getCode());
            logPub.setAmount(-mission.getReward());
            logPub.setMissionId(mission.getId());
            // todo 快照暂记的是发布者的“可用账户余额”
            //  后续资金流水表可加一个字段 account_type（账户类型）
            //  发布时：记一笔“可用账户”减少 100，再记一笔“冻结账户”增加 100。
            //  结算时：记一笔“冻结账户”减少 100。
            logPub.setBalanceAfter(publisher.getBalance());
            logPub.setRemark("任务结算成功，扣除冻结金：" + mission.getTitle());
            logPub.setCreateTime(LocalDateTime.now());
            transactionLogMapper.insert(logPub);

            // 3.4 增加接单者账户金额 (收益)
            int rows2 = userMapper.increaseBalance(mission.getAcceptorId(), mission.getReward());
            if (rows2 == 0) {
                throw new ServiceException("结算失败：接单用户账户异常");
            }

            // 3.5 加完钱后，查询接单者者的账户状态
            User acceptor = userMapper.selectById(mission.getAcceptorId());

            // 3.6 记录流水：接单者收入
            TransactionLog logAcc = new TransactionLog();
            logAcc.setUserId(mission.getAcceptorId());
            logAcc.setType(TransactionType.INCOME.getCode());
            logAcc.setAmount(mission.getReward());
            logAcc.setMissionId(mission.getId());
            logAcc.setBalanceAfter(acceptor.getBalance());
            logAcc.setRemark("完成悬赏任务奖励：" + mission.getTitle());
            logAcc.setCreateTime(LocalDateTime.now());
            transactionLogMapper.insert(logAcc);

            //更新任务状态
            missionMapper.updateStatus(mission.getId(), 3);

        } else {
            // === 分支 B: 审核驳回 (打回重做) ===

            // 状态回滚 -> 1 (进行中)
            // 钱不动，继续冻结在平台，等待弟子重新提交凭证
            missionMapper.updateStatus(mission.getId(), 1);
        }
        return true;
    }
}
