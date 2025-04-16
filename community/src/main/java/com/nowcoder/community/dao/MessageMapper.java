package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {
    //查询会话列表
    List<Message> selectConversation(int userId,int offSet,int limit);

    //查询会话数量
    int selectConversationCount(int userId);

    //查询某个会话的全部私信
    List<Message> selectLetters(String conversationId,int offSet,int limit);

    //查询私信数量
    int selectLetterCount(String conversationId);

    //查询未读私信数量
    int selectLetterUnreadCount(int userId,String conversationId);

    //新增消息
    int insertMessage(Message message);

    //更改消息状态
    int updateStatus(List<Integer> ids,int status);

    //查询最新系统通知
    Message selectLatestNotice(int userId,String topic);

    //查询系统通知数量
    int selectNoticeCount(int userId,String topic);

    //查询系统通知未读数量
    int selectNoticeUnreadCount(int userId,String topic);


    //查询所有通知
    List<Message> selectNotices(int userId,String topic,int offSet,int limit);



}
