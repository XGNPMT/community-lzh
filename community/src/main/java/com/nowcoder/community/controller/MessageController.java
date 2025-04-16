package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.HostHolder;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;



@Controller

public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表
    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        List<Message> conversationList = messageService.findConversation(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations = new ArrayList<>();
        if(conversationList!=null){
            for (Message message : conversationList) {
                Map<String,Object> map = new HashMap<>();
                map.put("conversation",message);
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                User target = user.getId()== message.getFromId()?userService.findUserById(message.getToId()):userService.findUserById(message.getFromId());
                map.put("target",target);
                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        int unreadLetterCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("unreadLetterCount",unreadLetterCount);

        int noticeUnreadCount = messageService.findUnreadNoticeCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "site/letter";
    }
    @GetMapping("/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId , Model model ,Page page){

        page.setRows(messageService.findLetterCount(conversationId));
        page.setLimit(5);
        List<Message> messageList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> messages = new ArrayList<>();
        if(messageList!=null){
            for (Message message : messageList) {
                Map<String,Object> map = new HashMap<>();
                map.put("message",message);
                map.put("fromUser",userService.findUserById(message.getFromId()));
                messages.add(map);
                model.addAttribute("messages",messages);
            }
        }
        List<Integer> ids = getIds(messageList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        model.addAttribute("target",getLetterUser(conversationId));
        return "site/letter-detail";
    }
    private List<Integer> getIds(List<Message> messageList){
        List<Integer> ids = new ArrayList<>();
        if(messageList!=null){
            for (Message message : messageList) {
                if(message.getToId()==hostHolder.getUser().getId()&&message.getStatus()==0){
                    ids.add(message.getId());
                }
            }

        }
        return ids;
    }
    private User getLetterUser(String conversionId){
        String[] split = conversionId.split("_");
        int id0 = Integer.parseInt(split[0]);
        int id1 = Integer.parseInt(split[1]);
        if(id0==hostHolder.getUser().getId()){
            return userService.findUserById(id1);
        }
        return userService.findUserById(id0);
    }

    //增加私信
    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter( String toName,String content) {
        User target=userService.findUserByName(toName);
        if(target==null){
            return CommunityUtil.getJSONString(404,"目标用户不存在！");
        }
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        message.setStatus(0);
        String conversationId =
        message.getFromId()<message.getToId()?message.getFromId()+"_"+ message.getToId():message.getToId()+"_"+ message.getFromId();

        message.setConversationId(conversationId);
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(200);
    }
    @GetMapping("/notice/list")
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();
        //查评论消息
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String,Object> messageVo = new HashMap<>();
        messageVo.put("message",message);
        if(message!=null){
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));
            messageVo.put("postId",data.get("postId"));
            messageVo.put("count",messageService.findNoticeCount(user.getId(), TOPIC_COMMENT));
            messageVo.put("unreadCount",messageService.findUnreadNoticeCount(user.getId(), TOPIC_COMMENT));
        }
        model.addAttribute("commentNotice",messageVo);

        //查点赞消息
        Message like = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        Map<String,Object> likeVo = new HashMap<>();
        likeVo.put("like",like);
        if(like!=null){
            String content = HtmlUtils.htmlUnescape(like.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            likeVo.put("entityType",data.get("entityType"));
            likeVo.put("entityId",data.get("entityId"));
            likeVo.put("user",userService.findUserById((Integer)data.get("userId")));
            likeVo.put("postId",data.get("postId"));
            likeVo.put("count",messageService.findNoticeCount(user.getId(), TOPIC_LIKE));
            likeVo.put("unreadCount",messageService.findUnreadNoticeCount(user.getId(), TOPIC_LIKE));
        }
        model.addAttribute("likeNotice",likeVo);
        //查询关注消息
        Message follow = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        Map<String,Object> followVo = new HashMap<>();
        followVo.put("follow",follow);
        if(follow!=null){
            String content = HtmlUtils.htmlUnescape(follow.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            followVo.put("entityType",data.get("entityType"));
            followVo.put("entityId",data.get("entityId"));
            followVo.put("user",userService.findUserById((Integer)data.get("userId")));
            followVo.put("count",messageService.findNoticeCount(user.getId(), TOPIC_LIKE));
            followVo.put("unreadCount",messageService.findUnreadNoticeCount(user.getId(), TOPIC_LIKE));
        }
        model.addAttribute("followNotice",followVo);
        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询通知未读消息数量
        int noticeUnreadCount = messageService.findUnreadNoticeCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "site/notice";
    }
    @GetMapping("/notice/detail/{topic}")
    public String getDetailNotice(@PathVariable("topic") String topic, Model model,Page page){
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        int rows = messageService.findNoticeCount(user.getId(), topic);
        page.setRows(rows);
        List<Message> notices = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>> noticeVoList = new ArrayList<>();
        if(notices!=null){
            for (Message notice : notices) {
                Map<String,Object> map = new HashMap<>();
                map.put("notice",notice);
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user",userService.findUserById((Integer)data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                map.put("fromUser",userService.findUserById(notice.getFromId()));
                noticeVoList.add(map);
                model.addAttribute("noticeVoList",noticeVoList);
            }
        }
        List<Integer> ids = getIds(notices);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }


        return "site/notice-detail";
    }



}
