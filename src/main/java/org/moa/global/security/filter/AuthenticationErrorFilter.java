package org.moa.global.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moa.global.security.util.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

@Component
public class AuthenticationErrorFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			// super.doFilter가 아닌 filterChain.doFilter를 호출
			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			if (!response.isCommitted()) {
				JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, "토큰의 유효시간이 지났습니다.");
			}
		} catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
			if (!response.isCommitted()) {
				JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, e.getMessage());
			}
		} catch (ServletException e) {
			if (!response.isCommitted()) {
				JsonResponse.sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
			}
			throw e; // ServletException은 다시 throw
		}
	}
}
