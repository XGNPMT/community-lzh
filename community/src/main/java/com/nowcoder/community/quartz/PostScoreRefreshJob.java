package com.nowcoder.community.quartz;

import com.nowcoder.community.CommunityApplication;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(CommunityApplication.class);

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LikeService likeService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    private static final Date epoch;
    static {
        try {
            epoch=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);
        if(operations.size()==0){
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数: "+operations.size());
        while(operations.size()>0){
            this.refresh((Integer) operations.pop());
        }
    }

    private void refresh(Integer pop) {
        DiscussPost post = discussPostService.findDiscussPostById(pop);
        if(post==null){
            logger.error("该帖子不存在：id="+pop);
        }
        //是否精华
        boolean wonderful = post.getStatus()==1;
        //评论数量
        int commentCount = post.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityCount(ENTITY_TYPE_POST, post.getId());
        //计算权重
        double w = (wonderful?75:0)+commentCount*10+likeCount*2;
        //分数 = 权重 + 发帖时间差值（天数）
        double score = Math.log10(Math.max(w,1))
                + (post.getCreateTime().getTime()-epoch.getTime())/(1000*3600*24);
        //更新帖子分数
        discussPostService.updateScore(pop,score);
        //同步elasticSearch;
        elasticsearchService.saveDiscussPost(post);
    }
}
