package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    private UserService userService;

    @Autowired
    CommentService commentService;

    @Autowired
    LikeService likeService;

    @Autowired
    EventProducer eventProducer;

    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title,String content){

        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJSONString(403,"您还没有登录！");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);
        //触发发帖事件
        Event event = new Event();
        event.setUserId(user.getId())
                .setTopic(TOPIC_PUBLISH)
                .setEntityId(discussPost.getId())
                .setEntityType(ENTITY_TYPE_POST);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(200,"发布成功！");

    }

    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") Integer discussPostId, Model model, Page page){
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        User user = userService.findUserById(post.getUserId());

        model.addAttribute("post",post);
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());
        //点赞数量
        long likeCount = likeService.findEntityCount(ENTITY_TYPE_POST, post.getId());
        model.addAttribute("likeCount",likeCount);
        //点赞数状态
        int likeStatus = hostHolder.getUser()==null?0:
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, post.getId());
        model.addAttribute("likeStatus",likeStatus);

        List<Comment> commentsByEntity =
                commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

        List<Map<String,Object>> commentVOList = new ArrayList<>();

        if(commentsByEntity!=null){
            for (Comment comment : commentsByEntity) {
                Map<String,Object> commentVO = new HashMap<>();
                commentVO.put("comment",comment);
                commentVO.put("user",userService.findUserById(comment.getUserId()));
                //点赞数量
                likeCount = likeService.findEntityCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("likeCount",likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser()==null?0:
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("likeStatus",likeStatus);
                //回复列表
                List<Comment> replys =
                        commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String,Object>> replyVOList = new ArrayList<>();
                if(replys!=null){
                    for (Comment reply : replys) {
                        Map<String,Object> replyVO = new HashMap<>();
                        replyVO.put("reply",reply);
                        replyVO.put("user",userService.findUserById(reply.getUserId()));
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVO.put("target",target);

                        //点赞数量
                        likeCount = likeService.findEntityCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVO.put("likeCount",likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser()==null?0:
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVO.put("likeStatus",likeStatus);
                        replyVOList.add(replyVO);
                    }
                }
                commentVO.put("replys",replyVOList);
                int commentCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("replyCount",commentCount);
                commentVOList.add(commentVO);
            }
        }
        model.addAttribute("comments",commentVOList);
        return "site/discuss-detail";
    }

    @PostMapping("/top")
    @ResponseBody
    public String setTop(int postId){
        discussPostService.updateStatus(postId,1);

        //触发置顶事件
        Event event = new Event();
        event.setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(200);
    }

    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int postId){
        discussPostService.updateType(postId,1);

        //触发精华事件
        Event event = new Event();
        event.setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(200);
    }

    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int postId){
        discussPostService.updateStatus(postId,2);

        //触发删除事件
        Event event = new Event();
        event.setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(200);
    }


}
