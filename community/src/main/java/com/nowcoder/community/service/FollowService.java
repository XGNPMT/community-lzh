package com.nowcoder.community.service;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    public void follow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                redisOperations.multi();
                    redisOperations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                redisOperations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                return redisOperations.exec();
            }
        });
    }
    public void unfollow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                redisOperations.multi();
                redisOperations.opsForZSet().remove(followerKey,userId);
                redisOperations.opsForZSet().remove(followeeKey,entityId);
                return redisOperations.exec();
            }
        });
    }

    //查询关注数量
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }
    //查询实体粉丝数量
    public long findFollowerCount(int entityType,int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    //查询当前用户是否关注某实体
    public boolean hasFollowed(int userId,int entityType,int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId)!=null;
    }

    //查询关注列表
    public List<Map<String,Object>> findFolloweeList(int userId, int entityType, Page page){
        List<Map<String,Object>> lists =new ArrayList<>();
        int limit = page.getLimit();
        int offSet = page.getOffset();
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Set<Integer> followees = redisTemplate.opsForZSet().range(followeeKey, offSet, offSet+limit-1);
        for (Integer followee : followees) {
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(followee);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, followee);
            map.put("followeeDate",new Date(score.longValue()));
            lists.add(map);
        }
        return lists;
    }
    //查询粉丝列表
    public List<Map<String,Object>> findFollowerList(int userId, int entityType, Page page){
        List<Map<String,Object>> lists =new ArrayList<>();
        int limit = page.getLimit();
        int offSet = page.getOffset();
        String followerKey = RedisKeyUtil.getFollowerKey(entityType,userId);
        Set<Integer> followers = redisTemplate.opsForZSet().range(followerKey, offSet, offSet+limit-1);
        for (Integer follower : followers) {
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(follower);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey, follower);
            map.put("followerDate",new Date(score.longValue()));
            lists.add(map);
        }
        return lists;
    }


}
