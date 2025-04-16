package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    DiscussPostService discussPostService;

    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        int userId = hostHolder.getUser().getId();
        comment.setUserId(userId);
        comment.setStatus(0);
        comment.setCreateTime(new Date(System.currentTimeMillis()));
        commentService.insertComment(comment);


        Event event = new Event();
        event.setTopic(TOPIC_COMMENT)
                .setUserId(userId)
                .setEntityId(discussPostId)
                .setEntityType(comment.getEntityType())
                .setData("postId",discussPostId);
        if(comment.getEntityType()==ENTITY_TYPE_POST){

            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            event = new Event();
            event.setUserId(comment.getUserId())
                    .setTopic(TOPIC_PUBLISH)
                    .setEntityId(comment.getId())
                    .setEntityType(ENTITY_TYPE_POST);
            eventProducer.fireEvent(event);
        }

        return "redirect:/discuss/detail/"+discussPostId;
    }

}
