package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.service.CustomUserDetailsService;
import org.moa.global.security.service.RedisTokenService;
import org.moa.global.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedJwtAuthenticationFilter extends OncePerRequestFilter {
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtProcessor jwtProcessor;
    private final UserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            
            try {
                // 1. JWT 자체 검증 (서명, 만료시간)
                if (!jwtProcessor.validateToken(token)) {
                    sendErrorResponse(response, "Invalid token signature");
                    return;
                }
                
                // 2. ⭐ Redis 블랙리스트 체크 (강제 로그아웃된 토큰인지)
                if (redisTokenService.isAccessTokenBlacklisted(token)) {
                    log.warn("Blacklisted token usage attempt");
                    sendErrorResponse(response, "Token has been revoked");
                    return;
                }
                
                // 3. JWT Claims 추출
                Claims claims = jwtProcessor.getClaims(token);
                
                // 4. 토큰 타입 확인
                String tokenType = claims.get("type", String.class);
                if (!"access".equals(tokenType)) {
                    sendErrorResponse(response, "Invalid token type");
                    return;
                }
                
                // 5. 만료 시간까지 남은 시간 계산 (Redis 캐싱용)
                long remainingTTL = claims.getExpiration().getTime() - System.currentTimeMillis();
                
                // 6. 인증 정보 설정
                Long memberId = Long.parseLong(claims.getSubject());
                UserDetails principal = ((CustomUserDetailsService) userDetailsService)
                        .loadUserByMemberId(memberId);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 7. 응답 헤더에 남은 TTL 정보 추가 (프론트엔드 참고용)
                response.setHeader("X-Token-TTL", String.valueOf(remainingTTL / 1000)); // 초 단위
                
                log.debug("Token validated - memberId: {}, TTL: {}s", 
                        memberId, remainingTTL / 1000);
                
            } catch (ExpiredJwtException e) {
                log.debug("Token expired");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("Token-Expired", "true");
                response.getWriter().write("{\"error\":\"Token expired\",\"code\":\"TOKEN_EXPIRED\"}");
                return;
                
            } catch (Exception e) {
                log.error("Token validation failed", e);
                sendErrorResponse(response, "Token validation failed");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}
