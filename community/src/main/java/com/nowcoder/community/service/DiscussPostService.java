package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.utils.CommunityUtil;
import com.nowcoder.community.utils.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffenie.posts.max-size}")
    private int maxSize;

    @Value("${caffenie.posts.expire-seconds}")
    private int expireSeconds;

    //caffenie核心接口Cache  ：LoadingCache，AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子数量缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init(){
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    @Nullable
                    public  List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key==null||key.length()==0){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String[] params = key.split(":");
                        if(params==null||params.length!=2){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        int offSet =Integer.parseInt(params[0]);
                        int limit  =Integer.parseInt(params[1]);
                        logger.info("load discussPost from DB.");
                        return discussPostMapper.selectDiscussPosts(0,offSet,limit,1);
                    }
                });
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        logger.info("load Rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });


    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit,int orderMode) {
        if(userId==0&&orderMode==1){
            postListCache.get(offset+":"+limit);
        }
        logger.info("load discussPost from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if (userId==0){
            postRowsCache.get(userId);
        }
        logger.info("load Rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //新增帖子
    public int addDiscussPost(DiscussPost post) {
        if(post==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        //转义html标签
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);


    }

    //查询帖子
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }
    //修改评论数量
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    //置顶帖子
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }
    //删除帖子
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }
    //更新帖子分数
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
