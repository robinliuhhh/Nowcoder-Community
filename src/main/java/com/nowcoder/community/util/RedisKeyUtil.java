package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWEE = "followee"; // 被关注者 用户/帖子/题目
    private static final String PREFIX_FOLLOWER = "follower"; // 粉丝
    private static final String PREFIX_KAPTCHA = "kaptcha";

    // 某个实体的赞
    // like:entity:entityType:entityId -> set(userId)
    // 存userId可以看到点赞人的信息 统计赞数时用set的size()方法
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    // 某个用户的赞
    // like:user:userId -> int
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个用户关注的实体 以当前时间作为排序
    // followee:userId:entityType -> zset(entityId, now)
    // getFolloweeKey: followee标识关注实体 userId:entityType表示某一用户关注的类型
    // value:          entityId now 关注的实体id 关注时间
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体拥有的粉丝
    // follower:entityType:entityId -> zset(userId, now)
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    // 登录验证码 owner代替userId作为凭证（未登录时还没有userId） owner随机生成即可 临时代替
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

}
