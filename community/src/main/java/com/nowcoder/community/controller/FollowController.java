package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;

import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    FollowService followService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private UserService userService;

    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType,int entityId){

        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType,entityId);
        Event event = new Event();
        event.setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityId(entityId)
                .setEntityType(entityType)
                //关注目标为用户
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(200,"已关注！");
    }

    @PostMapping("/unfollow")
    @ResponseBody
    public String unFollow(int entityType,int entityId){
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(), entityType,entityId);
        return CommunityUtil.getJSONString(200,"已取消关注！");
    }







}
