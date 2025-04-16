package com.nowcoder.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * @author xi_wang
 * @create 2025/4/7
 * @Description: 点赞业务
 */
@Service
public class LikeService {
    @Autowired
    RedisTemplate redisTemplate;

    //点赞业务
    public void like(int userId,int entityType,int entityId,int entityUserId){
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
//        //判断当前用户是否已经点赞
//        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//        if(isMember){
//            //如果已经点赞，就取消点赞
//            redisTemplate.opsForSet().remove(entityLikeKey,userId);
//        }else{
//            //如果没有点赞，就点赞
//            redisTemplate.opsForSet().add(entityLikeKey,userId);
//        }
        redisTemplate.execute(new SessionCallback()
        {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                Boolean isMember = redisOperations.opsForSet().isMember(entityLikeKey,userId);
                redisOperations.multi();
                if(isMember){
                    //如果已经点赞，就取消点赞
                    redisOperations.opsForSet().remove(entityLikeKey,userId);
                    redisOperations.opsForValue().decrement(userLikeKey);
                }else{
                    //如果没有点赞，就点赞
                    redisOperations.opsForSet().add(entityLikeKey,userId);
                    redisOperations.opsForValue().increment(userLikeKey);
                }
                return redisOperations.exec();
            }
        });
    }
    //统计点赞数量
    public long findEntityCount(int entityType,int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);

    }

    //查询点赞状态1表示已赞,0表示未赞
    public int findEntityLikeStatus(int userId,int entityType,int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId)?1:0;
    }

    //获得某个用户的获赞数
    public int findUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null?0:count.intValue();
    }
}
