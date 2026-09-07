package jude.carrot.chatserver.config;

import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    /**
     * 캐시 키 접두어. RedisCacheManager 기본 접두어는 "{cacheName}::" 라서
     * ChatKeyGenerator 의 데이터 키(chatRoom::{id} ZSet, chatMessage::{id} 대기 메시지)와 같은 키가 되어
     * ZADD 가 WRONGTYPE 으로 실패하고 ChatSyncService 의 SCAN 이 캐시 항목까지 집어간다. 네임스페이스를 분리한다.
     */
    private static final String CACHE_KEY_PREFIX = "cache::";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration("chatRoom", cacheConfiguration(ChatRoom.class))
                .withCacheConfiguration("chatParticipant", cacheConfiguration(ChatParticipant.class))
                .withCacheConfiguration("chatMessage", cacheConfiguration(ChatMessage.class))
                .build();
    }

    private RedisCacheConfiguration cacheConfiguration(Class<?> type) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .computePrefixWith(cacheName -> CACHE_KEY_PREFIX + cacheName + "::")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(type)));
    }
}
