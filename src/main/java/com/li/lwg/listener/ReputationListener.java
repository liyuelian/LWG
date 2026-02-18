package com.li.lwg.listener;

import com.li.lwg.dto.MissionSettledMsg;
import com.li.lwg.entity.ReputationLog;
import com.li.lwg.entity.User;
import com.li.lwg.enums.MissionDifficultyEnum;
import com.li.lwg.enums.ReputationSourceEnum;
import com.li.lwg.mapper.ReputationLogMapper;
import com.li.lwg.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 信誉系统消费者
 * 负责监听任务结算消息，更新用户信誉
 *
 * @author liyuelian
 */
@Slf4j
@Component
public class ReputationListener {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ReputationLogMapper reputationLogMapper;

    /**
     * 信誉队列
     */
    public static final String REPUTATION_QUEUE = "lwg.reputation.queue";

    /**
     * 任务结算处理信誉值
     */
    @RabbitListener(queues = REPUTATION_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleMessage(MissionSettledMsg msg, Channel channel, Message message) throws IOException {
        //获取消息标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("[信誉系统] 收到结算消息: UserId={}, MissionId={}, Pass={}",
                    msg.getUserId(), msg.getMissionId(), msg.isPass());
            Long userId = msg.getUserId();

            // 1. 幂等性检查,查询任务是否已经结算过
            int sourceType = ReputationSourceEnum.MISSION_SETTLED.getCode();
            int count = reputationLogMapper.countBySource(msg.getMissionId(), sourceType);
            if (count > 0) {
                log.info("[幂等拦截] 任务[{}] 已经结算过信誉，忽略重复消息。", msg.getMissionId());
                // 直接确认，跳过后续逻辑
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 用户数据
            User user = userMapper.selectById(userId);
            if (user == null) {
                // 手动确认当前消息，防止多次消费
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 计算信誉分数
            MissionDifficultyEnum difficulty = MissionDifficultyEnum.getByCode(msg.getDifficulty());
            int baseScore = difficulty.getScore();
            int change = msg.isPass() ? baseScore : -baseScore;

            // 4. 计算新信誉
            int currentRep = user.getReputation() != null ? user.getReputation() : 6000;
            int newRep = currentRep + change;

            // 5. 边界约束 [0, 12000]
            if (newRep > 12000) newRep = 12000;
            if (newRep < 0) newRep = 0;

            // 6. 更新用户信誉值
            int rows = userMapper.updateReputation(userId, newRep);
            if (rows == 0) {
                log.error("更新用户 userId:{} 信誉失败", userId);
                // 更新失败，拒绝不重试
                channel.basicReject(deliveryTag, false);
                return;
            }

            // 7. 记录信誉流水
            ReputationLog logEntry = new ReputationLog();
            logEntry.setUserId(userId);
            logEntry.setChangeScore(change);
            logEntry.setCurrentScore(newRep);
            // 来源类型: 1-任务结算
            logEntry.setSourceType(ReputationSourceEnum.MISSION_SETTLED.getCode());
            logEntry.setSourceId(msg.getMissionId());

            // 备注信息
            String action = msg.isPass() ? "完成" : "失败";
            String remark = String.format("%s任务(%s)：%s",
                    action, difficulty.getDesc(),
                    (change > 0 ? "+" : "") + change);
            logEntry.setRemark(remark);
            logEntry.setCreateTime(LocalDateTime.now());
            reputationLogMapper.insert(logEntry);

            log.info("[处理完成] 用户[{}] 变动[{}] 当前信誉值[{}]", userId, change, newRep);

            // 8. 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理信誉消息异常", e);
            // 失败，非不批量且不回队列
            channel.basicNack(deliveryTag, false, false);
        }
    }
}