package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    //新增帖子
    int insertDiscussPost(DiscussPost discussPost);

    //查询帖子详细信息
    DiscussPost selectDiscussPostById(int id);

    //修改帖子评论数量
    int updateCommentCount(int id, int commentCount);

    //修改帖子类型
    int updateType(int id, int type);

    //修改帖子状态
    int updateStatus(int id, int status);

}
