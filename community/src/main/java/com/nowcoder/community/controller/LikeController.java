package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
public class LikeController implements CommunityConstant {
    @Autowired
    LikeService likeService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;

    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType , int entityId,int entityUserId,int discussPostId){
        User user = hostHolder.getUser();
        int userId = user.getId();
        likeService.like(userId,entityType,entityId,entityUserId);

        long likeCount = likeService.findEntityCount(entityType, entityId);
        int entityLikeStatus = likeService.findEntityLikeStatus(userId, entityType, entityId);
        Map<String,Object> map = new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",entityLikeStatus);


        if(entityLikeStatus==1){
            Event event = new Event();
            event.setUserId(hostHolder.getUser().getId())
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setTopic(TOPIC_LIKE)
                    .setData("postId",discussPostId);
            eventProducer.fireEvent(event);
        }


        return CommunityUtil.getJSONString(200,null,map);

    }

}
