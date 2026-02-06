package com.zyyyys.culinarywhispers.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注Mapper接口
 * @author zyyyys
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
