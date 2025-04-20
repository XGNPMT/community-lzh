package com.nowcoder.community.utils;



public class RedisKeyUtil {
    //分隔符
    private static final String SPLIT=":";

    //点赞前缀
    private static final String PREFIX_ENTITY_LIKE="like:entity";

    //用户获赞前缀
    private static final String PREFIX_USER_LIKE="like:user";

    private static final String PREFIX_FOLLOWEE="followee";

    private static final String PREFIX_FOLLOWER="follower";

    private static final String PREFIX_KAPTCHA="kaptcha";

    private static final String PREFIX_TICKET="ticket";

    private static final String PREFIX_USER="user";

    private static final String PREFIX_UV="uv";

    private static final String PREFIX_DAU = "dau";

    private static final String PREFIX_POST = "post";




    //某个实体的赞
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
    }

    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET+SPLIT+ticket;
    }

    //统计获赞数量
    //like:user:userid -> int
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE+SPLIT+userId;
    }

    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA+SPLIT+owner;
    }

    //某个用户关注的实体
    //followee:userId:entityType -> zset(entityId,now)
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWEE+SPLIT+userId+SPLIT+entityType;
    }

    //某个实体拥有的粉丝
    //follower:entityType:entityId -> zset(userId,now)
    public static String getFollowerKey(int entityType,int entityID){
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityID;
    }

    public static String getUserKey(int userId) {
        return PREFIX_USER+SPLIT+userId;
    }

    //单日UV
    public static String getUVKey(String date){
        return PREFIX_UV+SPLIT+date;
    }
    //区间UV
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }
    //单日DAU
    public static String getDAUKey(String date){
        return PREFIX_DAU+SPLIT+date;
    }
    //区间DAU
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_DAU+SPLIT+startDate+SPLIT+endDate;
    }
    //分数
    public static String getPostKey(){
        return PREFIX_POST+SPLIT+"score";
    }


}
