package com.li.lwg.service.impl;

import com.li.lwg.dto.MissionAcceptReq;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.dto.MissionSubmitReq;
import com.li.lwg.dto.MissionAuditReq;
import com.li.lwg.enums.AssetType;
import com.li.lwg.enums.RealmEnum;
import com.li.lwg.enums.TransactionType;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
            throw new ServiceException("发布失败：您的灵石不足");
        }

        // 3. 插入任务
        Mission mission = new Mission();
        BeanUtils.copyProperties(req, mission);
        mission.setStatus(0); // 待接单
        mission.setVersion(0);
        missionMapper.insert(mission);

        // 拿到任务ID，用于关联流水
        Long missionId = mission.getId();

        // 4. 记录双向流水
        // 先查一次用户最新状态，确保 BalanceAfter 是准确的快照
        User user = userMapper.selectById(req.getPublisherId());
        // 两笔流水用同一个单号，方便关联
        String orderNo = UUID.randomUUID().toString();

        // 流水 A: 【可用余额】减少 (资产流出)
        TransactionLog logAvailable = new TransactionLog();
        logAvailable.setUserId(req.getPublisherId());
        logAvailable.setMissionId(missionId);
        logAvailable.setOrderNo(orderNo);
        logAvailable.setType(TransactionType.PUBLISH.getCode());       // 业务类型: 发布
        logAvailable.setAssetType(AssetType.AVAILABLE.getCode());      // 资产类型: 1-可用余额
        logAvailable.setAmount(-req.getReward());                      // 记负数 (-100)
        logAvailable.setBalanceAfter(user.getBalance());               // 记录【可用余额】快照
        logAvailable.setRemark("发布悬赏-资金划出：" + req.getTitle());
        transactionLogMapper.insert(logAvailable);

        // 流水 B: 【冻结余额】增加 (资产流入)
        TransactionLog logFrozen = new TransactionLog();
        logFrozen.setUserId(req.getPublisherId());
        logFrozen.setMissionId(missionId);
        logFrozen.setOrderNo(orderNo);
        logFrozen.setType(TransactionType.PUBLISH.getCode());          // 业务类型: 发布
        logFrozen.setAssetType(AssetType.FROZEN.getCode());            // 资产类型: 2-冻结余额
        logFrozen.setAmount(req.getReward());                          // 记正数 (+100)
        logFrozen.setBalanceAfter(user.getFrozenBalance());            // 记录【冻结余额】快照
        logFrozen.setRemark("发布悬赏-资金冻结：" + req.getTitle());
        transactionLogMapper.insert(logFrozen);

        return missionId;
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

        // 获取任务要求的最低境界 (默认1-炼气期)
        int minRealm = mission.getMinRealm() == null ? RealmEnum.LIAN_QI.getCode() : mission.getMinRealm();
        // 获取用户当前境界 (默认1-炼气期)
        int userRealm = acceptor.getRealm() == null ? RealmEnum.LIAN_QI.getCode() : acceptor.getRealm();

        if (userRealm < minRealm) {
            String requireRealm = RealmEnum.fromCode(mission.getMinRealm()).getDesc();
            throw new ServiceException("道友修为尚浅，此悬赏需【" + requireRealm + "】方可接取！");
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

            // 生成本次结算的统一订单号，方便关联一进一出
            String orderNo = UUID.randomUUID().toString();

            // 3.1 处理发布者 (扣减冻结资金)
            int rows1 = userMapper.decreaseFrozen(mission.getPublisherId(), mission.getReward());
            if (rows1 == 0) {
                throw new ServiceException("结算失败：发布者冻结资金异常");
            }

            // 查询发布者最新状态 (为了获取准确的 frozenBalance 快照)
            User publisher = userMapper.selectById(mission.getPublisherId());

            // 记录流水：发布者结算支出
            TransactionLog logPub = new TransactionLog();
            logPub.setUserId(mission.getPublisherId());
            logPub.setMissionId(mission.getId());
            logPub.setOrderNo(orderNo); // 关联订单号

            logPub.setType(TransactionType.SETTLEMENT.getCode()); // 业务类型: 结算支出
            logPub.setAssetType(AssetType.FROZEN.getCode());      // 资产类型: 冻结余额

            logPub.setAmount(-mission.getReward());               // 支出记负数
            logPub.setBalanceAfter(publisher.getFrozenBalance()); // 记录【冻结余额】快照
            logPub.setRemark("任务结算成功，扣除冻结金：" + mission.getTitle());
            logPub.setCreateTime(LocalDateTime.now());
            transactionLogMapper.insert(logPub);

            // 3.2 处理接单者 (增加可用余额)
            int rows2 = userMapper.increaseBalance(mission.getAcceptorId(), mission.getReward());
            if (rows2 == 0) {
                throw new ServiceException("结算失败：接单用户账户异常");
            }

            // 查询接单者最新状态
            User acceptor = userMapper.selectById(mission.getAcceptorId());

            // 记录流水：接单者收入
            TransactionLog logAcc = new TransactionLog();
            logAcc.setUserId(mission.getAcceptorId());
            logAcc.setMissionId(mission.getId());
            logAcc.setOrderNo(orderNo); // 关联订单号
            logAcc.setType(TransactionType.INCOME.getCode());    // 业务类型: 任务收益
            logAcc.setAssetType(AssetType.AVAILABLE.getCode());  // 资产类型: 可用余额
            logAcc.setAmount(mission.getReward());               // 收入记正数
            logAcc.setBalanceAfter(acceptor.getBalance());       // 记录【可用余额】快照
            logAcc.setRemark("完成悬赏任务奖励：" + mission.getTitle());
            logAcc.setCreateTime(LocalDateTime.now());
            transactionLogMapper.insert(logAcc);

            // 3.3 更新任务状态 -> 3 (已完成)
            missionMapper.updateStatus(mission.getId(), 3);

        } else {
            // === 分支 B: 审核驳回 (打回重做) ===

            // 状态回滚 -> 1 (进行中)
            // 钱不动，继续冻结在平台，等待弟子重新提交凭证
            missionMapper.updateStatus(mission.getId(), 1);
        }
        return true;
    }

    @Override
    public List<Mission> getMyMissions(Long userId, Integer type) {
        return missionMapper.selectMyMissions(userId, type);
    }
}
