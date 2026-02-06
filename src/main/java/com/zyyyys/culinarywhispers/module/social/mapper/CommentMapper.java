package com.zyyyys.culinarywhispers.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论Mapper接口
 * @author zyyyys
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
