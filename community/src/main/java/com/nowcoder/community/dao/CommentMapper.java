package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    //分页查询
    List<Comment> selectCommentByEntity(int entityType, int entityId,int offset,int limit);

    //查询评论数量
    int selectCountByEntity(int entityType, int entityId);

    //增加评论
    int insertComment(Comment comment);


    Comment selectCommentById(int id);
}
