package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    ElasticsearchService esService;
    @Autowired
    UserService userService;
    @Autowired
    LikeService likeService;
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        org.springframework.data.domain.Page<DiscussPost> discussPosts =
                esService.SearchDiscussPost(keyword, page.getCurrent(), page.getLimit());
        List<Map<String,Object>> lists= new ArrayList<>();
        if(discussPosts!=null){
            for (DiscussPost post : discussPosts) {
                Map<String,Object> map =new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("like",likeService.findEntityCount(ENTITY_TYPE_COMMENT, post.getId()));
                lists.add(map);
            }
        }
        model.addAttribute("discussposts",lists);
        model.addAttribute("keyword",keyword);
        page.setRows(discussPosts==null?0: (int) discussPosts.getTotalElements());
        page.setPath("/search/?keyword="+keyword);
        return "/site/search" ;
    }
}
