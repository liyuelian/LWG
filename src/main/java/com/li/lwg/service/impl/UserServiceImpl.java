package com.li.lwg.service.impl;

import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.enums.TransactionType;
import com.li.lwg.enums.AssetType;
import com.li.lwg.entity.User;
import com.li.lwg.exception.ServiceException;
import com.li.lwg.mapper.TransactionLogMapper;
import com.li.lwg.mapper.UserMapper;
import com.li.lwg.service.UserService;
import com.li.lwg.vo.FinanceOverviewVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TransactionLogMapper transactionLogMapper;

    @Override
    public List<TransactionLog> getMyTransactions(Long userId) {
        return transactionLogMapper.selectByUserId(userId);
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(UserRechargeReq req) {
        // 1.用户校验
        User user = userMapper.selectById(req.getUserId());
        if (user == null) {
            throw new ServiceException("道友不存在！");
        }
        // 是否有效账户 0-禁用 1-正常
        if (user.getStatus() != 1) {
            throw new ServiceException("账号已被封印，无法充值！");
        }
        // 金额上限校验
        if (req.getAmount() > 100000000L) {
            throw new ServiceException("单次充值数额过大，请联系宗门客服");
        }
        // 必须为正金额
        if (req.getAmount() <= 0) {
            throw new ServiceException("充值金额必须大于0！");
        }

        // 2. 原子更新 (成功率优先)
        int rows = userMapper.addBalance(req.getUserId(), req.getAmount());

        if (rows != 1) {
            throw new RuntimeException("充值失败，请稍后重试！");
        }

        // 3. 流水表插入
        // 虽然可能有并发导致快照跳跃，但这是 Trade-off
        // 只要保证流水不少，对账就能对上
        User latestUser = userMapper.selectById(req.getUserId());

        // 4. 记流水
        TransactionLog log = new TransactionLog();
        log.setUserId(user.getId());
        log.setAmount(req.getAmount());
        log.setBalanceAfter(latestUser.getBalance());
        log.setType(TransactionType.RECHARGE.getCode());
        log.setAssetType(AssetType.AVAILABLE.getCode());

        log.setOrderNo("RC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        log.setRemark("灵脉灌注 - 充值");
        log.setCreateTime(LocalDateTime.now());

        transactionLogMapper.insert(log);
    }

    @Override
    public FinanceOverviewVO getFinanceOverview(Long userId) {
        // 默认参数：本月1号 00:00:00
        LocalDate today = LocalDate.now();
        LocalDateTime firstDayOfMonth = today.withDayOfMonth(1).atStartOfDay();
        String monthStartStr = firstDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 类型参数从枚举中动态获取
        // 以后加新类型，只需改枚举，不用改这里
        List<Integer> incomeTypes = TransactionType.getRealIncomeTypes();
        List<Integer> expenseTypes = TransactionType.getRealExpenseTypes();

        // 兜底：防止空列表导致 SQL 报错 (如果没有定义支出类型)
        if (incomeTypes.isEmpty()) incomeTypes.add(-999);
        if (expenseTypes.isEmpty()) expenseTypes.add(-999);

        // 执行查询
        return transactionLogMapper.selectUserFinanceOverview(
                userId,
                monthStartStr,
                incomeTypes,
                expenseTypes
        );
    }
}