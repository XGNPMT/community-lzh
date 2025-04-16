package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private final static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private UserService userService;

    @Value("server.servlet.context-path")
    private  String contextPath;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/login")
    public String getLoginPage(){
        return "/site/login";
    }
    @GetMapping("/register")
    public String getRegisterPage(){return "/site/register";}

    @PostMapping("/register")
    public String register(Model model,User user){
        Map<String,Object> map = userService.regesiter(user);
        if(map==null||map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已向您发送一封邮件，请尽快激活！");
            model.addAttribute("target","/community/index");
            return "/site/operate-result";
        }
        else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("userEmailMsg",map.get("userEmailMsg"));
            return "/site/register";
        }
    }

    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model,
                             @PathParam("userId")int userId,
                             @PathParam("code") String code)
    {
        int result=userService.activation(userId,code);
        if(result==ACTIVATION_SUCCESS){
            model.addAttribute("msg","激活成功，您的帐号已可以正常使用！");
            model.addAttribute("target","/login");
        }
        if(result==ACTIVATION_REPEAT){
            model.addAttribute("msg","操作无效，您的帐号已被激活！");
            model.addAttribute("target","/index");

        }
        if(result==ACTIVATION_FAILURE){
            model.addAttribute("msg","操作失败，您的帐号激活失败！");
            model.addAttribute("target","/index");
        }
        return "/site/register";
    }

    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse httpServletResponse){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        httpServletResponse.addCookie(cookie);
        redisTemplate.opsForValue().set(RedisKeyUtil.getKaptchaKey(kaptchaOwner),text,60, TimeUnit.SECONDS);

        //验证码存入session
        //session.setAttribute("kaptcha",text);
        //图片输出到浏览器
        httpServletResponse.setContentType("image/png");
        try {
            OutputStream outputStream = httpServletResponse.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败！"+e.getMessage());
        }
    }

    @PostMapping("/login")
    public String login(String username,
                        String password,
                        String code,
                        boolean rememberMe,
                        Model model,
                        HttpServletResponse httpServletResponse,
                        @CookieValue("kaptchaOwner") String kaptchaOwner)
    {

        String Kaptcha = null;
        if(!StringUtils.isBlank(kaptchaOwner)){
            Kaptcha= (String) redisTemplate.opsForValue().get(RedisKeyUtil.getKaptchaKey(kaptchaOwner));
        }
        if(StringUtils.isBlank(Kaptcha) || StringUtils.isBlank(code)|| !Kaptcha.equals(code)){
            model.addAttribute("codeMsg","验证码不正确！");
            return "/site/login";
        }

        int expire = rememberMe?REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;

        Map<String, Object> map = userService.login(username, password, expire);

        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setMaxAge(expire);
            cookie.setPath(contextPath);
            httpServletResponse.addCookie(cookie);
            return "redirect:/index";
        }
        else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login";
        }

    }

    /*
        退出
     */
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }





}
