package com.zyyyys.culinarywhispers.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * @author zyyyys
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
