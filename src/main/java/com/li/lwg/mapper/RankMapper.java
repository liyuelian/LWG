package com.li.lwg.mapper;

import com.li.lwg.vo.RankEntryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RankMapper {
    List<RankEntryVO> selectReputationScores(@Param("afterId") long afterId, @Param("limit") int limit);

    List<RankEntryVO> selectCompletedScores(@Param("afterId") long afterId, @Param("limit") int limit);

    Long selectReputationScore(@Param("userId") Long userId);

    Long selectCompletedScore(@Param("userId") Long userId);

    List<RankEntryVO> selectProfiles(@Param("userIds") List<Long> userIds);
}
