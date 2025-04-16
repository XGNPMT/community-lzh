package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping ("/user")
public class UserController implements CommunityConstant {
    //实例化logger
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    //注入域名，项目名
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;

    //打开账号设置页面
    @GetMapping("/setting")
    @LoginRequired
    public String getSettingPage(){
        return "/site/setting";
    }
    @PostMapping("/upload")
    @LoginRequired
    public String uploadHeader(MultipartFile headImage, Model model){
        if(headImage==null){
            model.addAttribute("error","您还没有选择图片！");
            return "/site/setting";
        }
        String headerImageName = headImage.getOriginalFilename();
        //获取文件后缀名
        String suffix = headerImageName.substring(headerImageName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确！");
            return "/site/setting";
        }
        //生成随机文件名
        headerImageName = CommunityUtil.generateUUID() +suffix;
        File dest = new File(uploadPath+"/"+headerImageName);
        try {
            headImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("文件上传失败："+e.getMessage());
            throw new RuntimeException("文件上传失败，服务器异常！",e);
        }
        //更新用户头像路径
        User user = hostHolder.getUser();
        String headerUrl = domain+contextPath+"/user/header/"+headerImageName;
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";
    }

    @GetMapping("/header/{headerImageName}")
    public void getHeader(@PathVariable("headerImageName") String headerImageName, HttpServletResponse httpServletResponse){
        //服务器存放路径
        headerImageName = uploadPath+"/"+headerImageName;
        //文件后缀
        String suffix = headerImageName.substring(headerImageName.lastIndexOf("."));
        //响应图片
        httpServletResponse.setContentType("image/"+suffix);
        try (
                OutputStream outputStream = httpServletResponse.getOutputStream();
                FileInputStream fileInputStream = new FileInputStream(headerImageName);
                )
        {
            byte []buffer = new byte[1024];
            int b =0;
            while ((b=fileInputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,b);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public String updatePassword(Model model, @RequestParam String oldPassword,@RequestParam String newPassword){
        Map<String,Object> map = new HashMap<>();
        User user = hostHolder.getUser();
        map=userService.updatePassword(user.getId(),oldPassword,newPassword);
        if(map.isEmpty()){
            return "redirect:/logout";
        }else{
            model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

    /**
     * 个人主页
     */
    @GetMapping("/profile/{userId}")
    public String getUserInfo(Model model,@PathVariable("userId") int userId){
        User user = userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user",user);

        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);
        boolean hasFollowed = false;
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }
    /**
     * 查询关注列表
     */
    @GetMapping("/followees/{userId}")
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model){
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        page.setLimit(5);
        page.setRows((int) followeeCount);
        List<Map<String, Object>> followees = followService.findFolloweeList(userId, ENTITY_TYPE_USER,page);
        boolean hasFollowed = false;
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        model.addAttribute("followees",followees);
        model.addAttribute("preUser",userService.findUserById(userId));
        return "/site/followee";
    }
    /**
     * 查询粉丝列表
     */
    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model){
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        page.setLimit(5);
        page.setRows((int) followerCount);
        List<Map<String, Object>> followers = followService.findFollowerList(userId, ENTITY_TYPE_USER,page);
        boolean hasFollowed = false;
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        model.addAttribute("followers",followers);
        model.addAttribute("preUser",userService.findUserById(userId));
        return "/site/follower";
    }

}
