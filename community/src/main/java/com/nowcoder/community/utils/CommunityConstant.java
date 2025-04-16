package com.nowcoder.community.utils;

import org.apache.kafka.common.protocol.types.Field;

public interface CommunityConstant {
    // 激活成功
    int ACTIVATION_SUCCESS = 0;

    // 重复激活
    int ACTIVATION_REPEAT = 1;

    // 激活失败
    int ACTIVATION_FAILURE = 2;
    //默认登录凭证时间
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;
    //记住登陆凭证时间
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    //设置帖子类型
    int ENTITY_TYPE_POST = 1;
    int ENTITY_TYPE_COMMENT = 2;
    int ENTITY_TYPE_USER = 3;


    /**
     * 设置主题常量
     */
    String TOPIC_COMMENT = "comment";
    String TOPIC_LIKE = "like";
    String TOPIC_FOLLOW = "follow";
    String TOPIC_DELETE = "delete";
    String TOPIC_PUBLISH = "publish";

    //系统用户Id
    int SYSTEM_USER_ID = 1;

    //设置权限
    String AUTHORITY_USER = "user";
    String AUTHORITY_ADMIN = "admin";
    String AUTHORITY_MODERATOR = "moderator";

}
