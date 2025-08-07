package org.moa.global.security.controller;

import org.moa.global.security.service.RedisTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public/redis")
@RequiredArgsConstructor
public class RedisTestController {
    
    private final RedisTokenService redisTokenService;
    
    @GetMapping("/health")
    public ResponseEntity<?> checkRedisHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isAvailable = redisTokenService.isRedisAvailable();
            response.put("redis", isAvailable ? "Connected" : "Disconnected");
            response.put("status", isAvailable ? "OK" : "ERROR");
            
            if (isAvailable) {
                // 통계 정보도 가져오기
                Map<String, Object> stats = redisTokenService.getRedisStats();
                response.put("stats", stats);
            }
            
            log.info("Redis health check: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            response.put("redis", "Error");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/test-create")
    public ResponseEntity<?> testCreateToken() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 테스트용 토큰 생성
            var token = redisTokenService.createRefreshToken(1L);
            response.put("status", "OK");
            response.put("token", token.getToken().substring(0, 8) + "...");
            response.put("family", token.getTokenFamily().substring(0, 8) + "...");
            
            log.info("Test token created: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Test token creation failed", e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.status(500).body(response);
        }
    }
}