package com.li.lwg.service.impl;

import com.li.lwg.common.PageResult;
import com.li.lwg.dto.TransactionPageReq;
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
import com.li.lwg.vo.FinanceChartVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Override
    public PageResult<TransactionLog> getTransactionPage(TransactionPageReq req) {
        // 设置 资金动账类型枚举 类型列表
        if ("income".equals(req.getCategory())) {
            req.setTypeList(TransactionType.getRealIncomeTypes());
        } else if ("expense".equals(req.getCategory())) {
            req.setTypeList(TransactionType.getRealExpenseTypes());
        } else if ("locked".equals(req.getCategory())) {
            req.setTypeList(TransactionType.getInternalTypes());
        }

        // offset 会在 DTO 内部自动计算
        List<TransactionLog> list = transactionLogMapper.selectTransactionPage(req);
        Long total = transactionLogMapper.countTransaction(req);

        return new PageResult<>(total, list);
    }

    @Override
    public FinanceChartVO getFinanceCharts(Long userId) {
        // 1. 时间范围：最近 12 个月
        LocalDate today = LocalDate.now();
        // 比如现在是 2026-02，起始就是 2025-03-01
        LocalDate startMonth = today.minusMonths(11).withDayOfMonth(1);
        String startTimeStr = startMonth.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 2. 准备动态类型 (从枚举获取)
        // 真实收入
        List<Integer> incomeTypes = TransactionType.getRealIncomeTypes();
        // 真实支出
        List<Integer> expenseTypes = TransactionType.getRealExpenseTypes();

        // 3. 查趋势图数据 (DB返回的是稀疏数据，可能缺月份)
        List<Map<String, Object>> rawTrend = transactionLogMapper.selectYearlyTrend(
                userId, startTimeStr, incomeTypes, expenseTypes
        );

        // --- 核心补零逻辑 Start ---
        // 将 DB 数据转为 Map 方便查找
        Map<String, Long> incomeMap = new HashMap<>();
        Map<String, Long> expenseMap = new HashMap<>();
        for (Map<String, Object> m : rawTrend) {
            String month = (String) m.get("month");
            // 使用 BigDecimal 转 Long 防止类型转换报错
            incomeMap.put(month, new BigDecimal(m.get("income").toString()).longValue());
            expenseMap.put(month, new BigDecimal(m.get("expense").toString()).longValue());
        }

        // 构造连续的 12 个月
        List<String> months = new ArrayList<>();
        List<Long> incomeList = new ArrayList<>();
        List<Long> expenseList = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 0; i < 12; i++) {
            // 生成 "2025-03"
            String key = startMonth.plusMonths(i).format(fmt);
            months.add(key);
            // 有数据填数据，没数据填 0
            incomeList.add(incomeMap.getOrDefault(key, 0L));
            expenseList.add(expenseMap.getOrDefault(key, 0L));
        }
        // --- 核心补零逻辑 End ---

        // 4. 查饼图数据
        List<Integer> allRealTypes = new ArrayList<>();
        allRealTypes.addAll(incomeTypes);
        allRealTypes.addAll(expenseTypes);

        List<Map<String, Object>> rawPie = transactionLogMapper.selectTypeDistribution(userId, allRealTypes);

        // 4.1 转换饼图 (ID 转 中文名)
        List<FinanceChartVO.PieItem> pieList = new ArrayList<>();
        if (rawPie != null) {
            for (Map<String, Object> map : rawPie) {
                Integer typeId = (Integer) map.get("type");
                Long value = new BigDecimal(map.get("total").toString()).longValue();

                // 查枚举获取中文描述
                String name = Arrays.stream(TransactionType.values())
                        .filter(e -> e.getCode() == typeId)
                        .map(TransactionType::getDesc)
                        .findFirst()
                        .orElse("未知类型");

                pieList.add(new FinanceChartVO.PieItem(name, value));
            }
        }

        // 5. 组装返回
        FinanceChartVO vo = new FinanceChartVO();
        vo.setTrendMonths(months);
        vo.setTrendIncome(incomeList);
        vo.setTrendExpense(expenseList);
        vo.setPieData(pieList);

        return vo;
    }
}