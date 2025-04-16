package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.MailClient;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    //注入项目名
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private RedisTemplate redisTemplate;

//    @Autowired
//    LoginTicketMapper loginTicketMapper;
    /*
        登录业务接口
     */

    public User findUserById(int id) {
        User user = getCache(id);
        if(user==null){
            user=initCache(id);
        }
        return user;
        //return userMapper.selectById(id);
    }

    /*
        注册业务接口
     */
    public Map<String,Object> regesiter(User user){
        Map<String,Object> map = new HashMap<>();
        //对空值处理
        if(user == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","用户名不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","用户密码不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("userMailMsg","用户名不能为空！");
            return map;
        }
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg","用户名已被注册！");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("userMailMsg","邮箱已被注册！");
            return map;
        }
        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        String pas=CommunityUtil.md5(user.getPassword()+user.getSalt());
        user.setPassword(pas);

        //设置用户类型0（普通用户）
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        //给用户发送激活邮件
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        String url = domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("mail/activation",context);
        mailClient.sendmail(user.getEmail(), "激活账户",content);

        return map;
    }

    /*
        激活业务接口
     */
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }
        if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }
        else{
            return ACTIVATION_FAILURE;
        }
    }

    /*
        登录接口
     */
    public Map<String,Object> login(String username,String password,int expire){
        Map<String,Object> map = new HashMap<>();
        if(StringUtils.isBlank(username)){
           map.put("usernameMsg","用户名不能为空！");
           return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空！");
            return map;
        }
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg","用户不存在！");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usernameMsg","用户未激活！");
            return map;
        }

        //密码校验
        if(!CommunityUtil.md5(password+user.getSalt()).equals(user.getPassword())){
            map.put("passwordMsg","密码错误！");
            return map;
        }
        //生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expire*1000));
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);
        map.put("ticket",loginTicket.getTicket());



        return map;
    }

    /*
        退出
     */
    public void logout(String ticket){
//        loginTicketMapper.updateStatus(ticket,1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket= (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    /**
     * 查询登陆凭证接口
     */
    public LoginTicket findLoginTicket(String ticket){
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 更新用户头像
     */
    public int updateHeader(int userId,String headerUrl){
        int rows = userMapper.updateHeader(userId,headerUrl);
        clearCache(userId);
        return rows;

    }

    /**
     * 修改密码
     */
    public Map<String,Object> updatePassword(int userId,String oldPassword,String newPassword){
        Map<String,Object> map = new HashMap<>();
        User user = userMapper.selectById(userId);
        if(StringUtils.isBlank(oldPassword)){
            map.put("oldPasswordMsg","请输入原密码！");
            return map;
        }
        if(StringUtils.isBlank(newPassword)){
            map.put("newPasswordMsg","请输入新密码！");
            return map;
        }
        if(!CommunityUtil.md5(oldPassword+user.getSalt()).equals(user.getPassword())){
            map.put("oldPasswordMsg","旧密码错误！");
            return map;
        }
        if(newPassword.length()<3){
            map.put("newPasswordMsg","密码长度不能小于3！");
            return map;
        }
        userMapper.updatePassword(userId,CommunityUtil.md5(newPassword+user.getSalt()));
        return map;
    }

    public User findUserByName(String toName) {
        return userMapper.selectByName(toName);
    }

    public List<User> findUserByIds(Set<Integer> followees,int offSet,int limit) {
        return userMapper.selectByIds(followees,offSet,limit);
    }
    //查询缓存
    public User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    //更新缓存
    public User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user);
        return user;
    }
    //数据更新时，删除缓存
    public void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthority(int userId){
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}

