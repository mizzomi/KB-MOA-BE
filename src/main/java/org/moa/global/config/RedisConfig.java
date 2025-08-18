package org.moa.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 설정 클래스
 * RTR (Refresh Token Rotation) 구현을 위한 Redis 연결 및 템플릿 설정
 */
@Configuration
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.timeout:2000}")
    private long timeout;

    /**
     * 로컬 환경용 Redis 연결 설정 (Docker 미사용)
     */
    @Bean
    @Profile({"default", "local"})
    public RedisConnectionFactory redisConnectionFactoryLocal() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName("localhost");
        redisConfig.setPort(6379);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(timeout))
                                .build())
                        .build())
                .commandTimeout(Duration.ofMillis(timeout))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * 배포 환경용 Redis 연결 설정 (Docker 사용)
     */
    @Bean
    @Profile("docker")
    public RedisConnectionFactory redisConnectionFactoryDocker() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(timeout))
                                .build())
                        .build())
                .commandTimeout(Duration.ofMillis(timeout))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * RedisTemplate 설정
     * JWT Token 및 관련 데이터 저장을 위한 템플릿
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Jackson ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Serializer 설정
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Key-Value Serializer 설정
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
