package org.moa.global.security.config;

import org.moa.global.security.filter.AuthenticationErrorFilter;
import org.moa.global.security.filter.JwtAuthenticationFilter;
import org.moa.global.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.moa.global.security.filter.TokenAutoRefreshFilter;
import org.moa.global.security.handler.CustomAccessDeniedHandler;
import org.moa.global.security.handler.CustomAuthenticationEntryPoint;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
// @MapperScan(basePackages = {"org.moa.member.mapper"})
@ComponentScan(basePackages = {"org.moa.global.security"})
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final UserDetailsService userDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final AuthenticationErrorFilter authenticationErrorFilter;
	private final CustomAccessDeniedHandler accessDeniedHandler;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;

	@Autowired
	private JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter;
	
	@Autowired(required = false)
	private TokenAutoRefreshFilter tokenAutoRefreshFilter;

	// BCryptPasswordEncoder 빈 등록
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // 보안 설정의 일환
	}

	@Bean
	@SuppressWarnings("deprecation") // NoOpPasswordEncoder는 deprecated
	public NoOpPasswordEncoder encoder() {
		return (NoOpPasswordEncoder)NoOpPasswordEncoder.getInstance();
	}

	// AuthenticationManager 빈 등록
	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManager();
	}

	// cross origin 접근 허용 (백엔드 서버와 다른 origin에서의 접근 허용) => ex) 백엔드서버(로컬) - 프론트엔드(browser)
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		
		// 허용할 Origin 설정 (프론트엔드 도메인 추가)
		config.addAllowedOrigin("http://localhost:5173");  // 로컬 개발
		config.addAllowedOrigin("https://localhost:5173"); //로컬 개발 https
		config.addAllowedOrigin("http://localhost:3000");   // 로컬 개발 (React 기본)
		config.addAllowedOrigin("https://kb-moa-fe.vercel.app");  // Vercel 프론트엔드 배포
		config.addAllowedOrigin("https://kbmoa.store");     // 배포 백엔드 (필요시)
		
		// 또는 모든 Origin 허용 (개발 단계에서만 사용, 프로덕션에서는 특정 도메인만 허용)
		// config.addAllowedOriginPattern("*");
		
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.addExposedHeader("Authorization");
		config.addExposedHeader("Set-Cookie");
		config.addExposedHeader("X-New-Access-Token");  // 자동 갱신 토큰 헤더 노출
		config.addExposedHeader("X-Token-Refreshed");    // 토큰 갱신 여부 헤더 노출
		config.setMaxAge(3600L); // preflight 결과 캐시
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	// 문자셋 필터
	public CharacterEncodingFilter encodingFilter() {
		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
		encodingFilter.setEncoding("UTF-8");
		encodingFilter.setForceEncoding(true);
		return encodingFilter;
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		// 기본 필터 설정
		http
			.addFilterBefore(corsFilter(), ChannelProcessingFilter.class)
			.addFilterBefore(encodingFilter(), CsrfFilter.class);

		// 커스텀 필터 체인 구성
		// 순서: AuthenticationErrorFilter -> JwtUsernamePasswordAuthenticationFilter -> TokenAutoRefreshFilter -> JwtAuthenticationFilter
		
		// 1. 인증 에러 처리 필터 (가장 먼저)
		http.addFilterBefore(authenticationErrorFilter, LogoutFilter.class);
		
		// 2. 로그인 처리 필터 (UsernamePasswordAuthenticationFilter 위치에)
		http.addFilterAt(jwtUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		
		// 3. Token Auto Refresh Filter (로그인 필터 다음, JWT 필터 전)
		if (tokenAutoRefreshFilter != null) {
			http.addFilterAfter(tokenAutoRefreshFilter, UsernamePasswordAuthenticationFilter.class);
		}
		
		// 4. JWT 인증 필터 (마지막)
		http.addFilterAfter(jwtAuthenticationFilter, 
			tokenAutoRefreshFilter != null ? TokenAutoRefreshFilter.class : UsernamePasswordAuthenticationFilter.class);

		// Exception Handling
		http
			.cors().and()
			.exceptionHandling()
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler);

		// Security 기본 설정
		http.httpBasic().disable()
			.csrf().disable()
			.formLogin().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// URL별 권한 설정
		http
			.authorizeRequests()
			// 정적 리소스 허용
			.antMatchers("/assets/**", "/").permitAll()
			// OPTIONS 요청 허용
			.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
			// API 경로별 권한 설정
			.antMatchers("/api/public/**").permitAll()
			// 역할별 접근 제한
			.antMatchers("/api/member").access("hasRole('ROLE_USER')")
			.antMatchers("/api/admin").access("hasRole('ROLE_ADMIN')")
			.antMatchers("/api/owner/**").access("hasRole('ROLE_OWNER')")
			// 나머지는 인증 필요
			.anyRequest().authenticated();
	}

	// Authentication Manager 구성
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
			.userDetailsService(userDetailsService)
			// .passwordEncoder(passwordEncoder()); // 암호화 방식
			.passwordEncoder(encoder()); // 암호화 방식
	}
}
