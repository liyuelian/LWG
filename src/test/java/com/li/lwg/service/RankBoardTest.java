package com.li.lwg.service;

import com.li.lwg.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.li.lwg.service.impl.RankIndex;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 真实 MySQL 上验证窗口排名与 HTTP 契约，所有夹具仅写一次性测试容器并回滚。 */
@AutoConfigureMockMvc
@Transactional
class RankBoardTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RankIndex rankIndex;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private SqlSessionTemplate sqlSession;

    @BeforeEach
    void fixtures() {
        jdbc.update("DELETE FROM t_mission");
        jdbc.update("DELETE FROM t_user");
        for (long id = 1; id <= 55; id++) {
            jdbc.update("INSERT INTO t_user (id, username, password, realm, reputation, status) VALUES (?, ?, ?, 1, ?, ?)",
                    id, "道友" + id, "never-expose-this", id <= 2 ? 9000 : 6000 + id, id == 55 ? 0 : 1);
        }
        jdbc.update("UPDATE t_user SET reputation = NULL WHERE id = 3");
        mission(1, 3);
        mission(1, 3);
        mission(2, 3);
        mission(2, 3);
        mission(4, 3);
        mission(55, 3);
        for (int state : new int[]{0, 1, 2, 4}) {
            mission(3, state);
        }
        rankIndex.rebuild();
    }

    private void mission(long acceptor, int state) {
        jdbc.update("INSERT INTO t_mission (title, reward, publisher_id, acceptor_id, status) VALUES ('测试悬赏', 10, 54, ?, ?)",
                acceptor, state);
    }

    @Test
    void reputationTiesAreStableAndPersonalRankIncludesUsersOutsideTop50() throws Exception {
        mvc.perform(get("/api/rank/board").param("userId", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("reputation"))
                .andExpect(jsonPath("$.data.total").value(54))
                .andExpect(jsonPath("$.data.entries.length()").value(50))
                .andExpect(jsonPath("$.data.entries[0].userId").value(1))
                .andExpect(jsonPath("$.data.entries[1].userId").value(2))
                .andExpect(jsonPath("$.data.entries[0].rank").value(1))
                .andExpect(jsonPath("$.data.entries[1].rank").value(1))
                .andExpect(jsonPath("$.data.entries[2].rank").value(3))
                .andExpect(jsonPath("$.data.myRank.rank").value(54))
                .andExpect(jsonPath("$.data.myRank.score").value(6000))
                .andExpect(jsonPath("$.data.entries[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.entries[0].balance").doesNotExist());
    }

    @Test
    void completedBoardOnlyCountsAcceptedCompletedMissionsAndExcludesDisabledUsers() throws Exception {
        mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", "4").param("limit", "2"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.entries.length()").value(2))
                .andExpect(jsonPath("$.data.entries[0].userId").value(1))
                .andExpect(jsonPath("$.data.entries[0].score").value(2))
                .andExpect(jsonPath("$.data.entries[1].rank").value(1))
                .andExpect(jsonPath("$.data.myRank.rank").value(3))
                .andExpect(jsonPath("$.data.myRank.score").value(1));
    }

    @Test
    void absentAndIneligibleUsersHaveNoRank() throws Exception {
        for (String id : new String[]{"3", "55", "999"}) {
            mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", id))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.myRank").doesNotExist());
        }
        mvc.perform(get("/api/rank/board"))
                .andExpect(jsonPath("$.data.myRank").doesNotExist());
    }

    @Test
    void emptyBoardReturnsEmptyList() throws Exception {
        jdbc.update("DELETE FROM t_mission");
        // 本测试在同一事务中通过 JdbcTemplate 改数据，清掉 MyBatis 一级查询缓存。
        sqlSession.clearCache();
        rankIndex.rebuild();
        mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", "1"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.entries").isEmpty())
                .andExpect(jsonPath("$.data.myRank").doesNotExist());
        jdbc.update("UPDATE t_user SET status = 0");
        sqlSession.clearCache();
        rankIndex.rebuild();
        mvc.perform(get("/api/rank/board"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.entries").isEmpty());
    }

    @Test
    void invalidParametersReturnReadableBusinessErrors() throws Exception {
        for (String limit : new String[]{"0", "101", "-1", "abc", "999999999999"}) {
            mvc.perform(get("/api/rank/board").param("limit", limit))
                    .andExpect(jsonPath("$.code").value(400));
        }
        for (String id : new String[]{"0", "-1", "abc"}) {
            mvc.perform(get("/api/rank/board").param("userId", id))
                    .andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(get("/api/rank/board").param("type", "unknown"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void refreshedBoardReflectsSettlementAndReputationChanges() throws Exception {
        jdbc.update("UPDATE t_mission SET status = 3 WHERE acceptor_id = 3 AND status = 2");
        jdbc.update("UPDATE t_user SET reputation = 10000 WHERE id = 3");
        // 查询只读 Redis：数据库变化在投影更新前不会悄悄触发 MySQL 全榜聚合。
        mvc.perform(get("/api/rank/board").param("userId", "3"))
                .andExpect(jsonPath("$.data.myRank.rank").value(54));
        mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", "3"))
                .andExpect(jsonPath("$.data.myRank").doesNotExist());
        rankIndex.refreshCompleted(3L);
        rankIndex.refreshReputation(3L);
        mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", "3"))
                .andExpect(jsonPath("$.data.myRank.score").value(1))
                .andExpect(jsonPath("$.data.myRank.rank").value(3));
        mvc.perform(get("/api/rank/board").param("userId", "3"))
                .andExpect(jsonPath("$.data.myRank.rank").value(1))
                .andExpect(jsonPath("$.data.entries[0].userId").value(3));
    }

    @Test
    void missingRedisProjectionRebuildsFromMysql() throws Exception {
        redis.delete("lwg:rank:v1:ready");
        redis.delete("lwg:rank:v1:reputation");
        redis.delete("lwg:rank:v1:completed");
        mvc.perform(get("/api/rank/board").param("type", "completed").param("userId", "4"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.myRank.rank").value(3));
        mvc.perform(get("/api/rank/board").param("userId", "3"))
                .andExpect(jsonPath("$.data.myRank.rank").value(54));
    }
}
