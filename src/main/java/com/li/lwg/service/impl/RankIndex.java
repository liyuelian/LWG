package com.li.lwg.service.impl;

import com.li.lwg.enums.RankTypeEnum;
import com.li.lwg.mapper.RankMapper;
import com.li.lwg.vo.RankEntryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** MySQL 是事实来源；Redis ZSET 是可重建的排行榜投影。 */
@Slf4j
@Component
public class RankIndex implements ApplicationRunner {
    private static final int BATCH_SIZE = 1000;
    private static final String REPUTATION_KEY = "lwg:rank:v1:reputation";
    private static final String COMPLETED_KEY = "lwg:rank:v1:completed";
    private static final String READY_KEY = "lwg:rank:v1:ready";
    private static final DefaultRedisScript<Long> PUBLISH_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then redis.call('RENAME', KEYS[1], KEYS[3]) "
                    + "else redis.call('DEL', KEYS[3]) end "
                    + "if redis.call('EXISTS', KEYS[2]) == 1 then redis.call('RENAME', KEYS[2], KEYS[4]) "
                    + "else redis.call('DEL', KEYS[4]) end "
                    + "redis.call('SET', KEYS[5], '1') return 1", Long.class);

    @Resource
    private RankMapper rankMapper;
    @Resource
    private StringRedisTemplate redis;

    @Override
    public void run(ApplicationArguments args) {
        rebuild();
    }

    /** 每日校准，修复服务中断或 Redis 更新失败期间漏掉的增量。 */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Shanghai")
    public void scheduledRebuild() {
        rebuild();
    }

    public synchronized void ensureReady() {
        if (!Boolean.TRUE.equals(redis.hasKey(READY_KEY))) {
            rebuild();
        }
    }

    /** 临时键分批建榜，最后 Lua 原子替换两张榜；空榜也由 ready 键标记。 */
    public synchronized void rebuild() {
        String suffix = ":build:" + UUID.randomUUID();
        String reputationBuild = REPUTATION_KEY + suffix;
        String completedBuild = COMPLETED_KEY + suffix;
        try {
            long reputationCount = fill(reputationBuild, true);
            long completedCount = fill(completedBuild, false);
            redis.execute(PUBLISH_SCRIPT, List.of(
                    reputationBuild, completedBuild, REPUTATION_KEY, COMPLETED_KEY, READY_KEY));
            log.info("天道碑重建完成：信誉 {} 人，悬赏完成 {} 人", reputationCount, completedCount);
        } finally {
            redis.delete(List.of(reputationBuild, completedBuild));
        }
    }

    private long fill(String key, boolean reputation) {
        long afterId = 0;
        long count = 0;
        while (true) {
            List<RankEntryVO> batch = reputation
                    ? rankMapper.selectReputationScores(afterId, BATCH_SIZE)
                    : rankMapper.selectCompletedScores(afterId, BATCH_SIZE);
            if (batch.isEmpty()) {
                return count;
            }
            Set<TypedTuple<String>> scores = new HashSet<>();
            for (RankEntryVO entry : batch) {
                scores.add(new DefaultTypedTuple<>(member(entry.getUserId()), entry.getScore().doubleValue()));
            }
            redis.opsForZSet().add(key, scores);
            redis.expire(key, Duration.ofHours(1));
            count += batch.size();
            afterId = batch.get(batch.size() - 1).getUserId();
        }
    }

    /** 提交后读取数据库真实分数并回写；不靠 ZINCRBY，重复消息不会重复加分。 */
    public synchronized void refreshCompleted(Long userId) {
        ensureReady();
        Long score = rankMapper.selectCompletedScore(userId);
        update(COMPLETED_KEY, userId, score != null && score > 0 ? score : null);
    }

    public synchronized void refreshReputation(Long userId) {
        ensureReady();
        update(REPUTATION_KEY, userId, rankMapper.selectReputationScore(userId));
    }

    private void update(String key, Long userId, Long score) {
        if (score == null) {
            redis.opsForZSet().remove(key, member(userId));
        } else {
            redis.opsForZSet().add(key, member(userId), score.doubleValue());
        }
    }

    public long total(RankTypeEnum type) {
        ensureReady();
        Long size = redis.opsForZSet().size(key(type));
        return size == null ? 0 : size;
    }

    public List<RankEntryVO> top(RankTypeEnum type, int limit) {
        ensureReady();
        Set<TypedTuple<String>> values = redis.opsForZSet().reverseRangeWithScores(key(type), 0, limit - 1);
        List<RankEntryVO> result = new ArrayList<>();
        Map<Long, Long> ranksByScore = new HashMap<>();
        if (values != null) {
            for (TypedTuple<String> value : values) {
                RankEntryVO entry = new RankEntryVO();
                entry.setUserId(userId(value.getValue()));
                entry.setScore(value.getScore().longValue());
                entry.setRank(ranksByScore.computeIfAbsent(entry.getScore(), score -> rank(type, score)));
                result.add(entry);
            }
        }
        return result;
    }

    public RankEntryVO personal(RankTypeEnum type, Long id) {
        ensureReady();
        Double score = redis.opsForZSet().score(key(type), member(id));
        if (score == null) {
            return null;
        }
        RankEntryVO entry = new RankEntryVO();
        entry.setUserId(id);
        entry.setScore(score.longValue());
        entry.setRank(rank(type, entry.getScore()));
        return entry;
    }

    /** 分数均为整数；ZCOUNT(score + 1, +inf) 得到严格高于本分数的人数。 */
    private long rank(RankTypeEnum type, long score) {
        Long higher = redis.opsForZSet().count(key(type), score + 1, Double.MAX_VALUE);
        return (higher == null ? 0 : higher) + 1;
    }

    private String key(RankTypeEnum type) {
        return type == RankTypeEnum.REPUTATION ? REPUTATION_KEY : COMPLETED_KEY;
    }

    /** ZREVRANGE 同分按 member 逆字典序；反转 ID 后即为用户 ID 升序。 */
    private String member(long userId) {
        return String.format(Locale.ROOT, "%019d", Long.MAX_VALUE - userId);
    }

    private long userId(String member) {
        return Long.MAX_VALUE - Long.parseLong(member);
    }
}
