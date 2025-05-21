package com.boot.cms.config;

import com.boot.cms.entity.auth.LoginEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /*
    @Bean
    public RedisTemplate<String, LoginEntity> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, LoginEntity> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(LoginEntity.class));
        return template;
    }
    */
}