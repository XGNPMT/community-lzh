package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate template = new RedisTemplate();

        template.setConnectionFactory(factory);
        //指定序列化方式
        //设置Key的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        //设置Value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //设置Hash的Key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //设置Hash的Value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }
}
