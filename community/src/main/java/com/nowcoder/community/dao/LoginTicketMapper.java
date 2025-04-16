package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Deprecated
@Mapper
public interface LoginTicketMapper {
    //插入凭证
    @Insert("insert into login_ticket(user_id,ticket,status,expired) values (#{userId},#{ticket},#{status},#{expired})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);
    //查询凭证
    @Select("select id,user_id,ticket,status,expired from login_ticket where ticket=#{ticket}")
    LoginTicket selectByTicket(String ticket);
    //更改数据的状态
    @Update("update login_ticket set status=#{status} where ticket=#{ticket}")
    int updateStatus(String ticket,int status);
}
