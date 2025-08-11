package org.moa.global.security.config;

import org.moa.global.security.filter.AuthenticationErrorFilter;
import org.moa.global.security.filter.JwtAuthenticationFilter;
import org.moa.global.security.filter.JwtUsernamePasswordAuthenticationFilter;
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
		config.addAllowedOrigin("http://localhost:3000");   // 로컬 개발 (React 기본)
		config.addAllowedOrigin("https://moa-fe-bhyd.vercel.app");  // Vercel 프론트엔드 배포
		config.addAllowedOrigin("https://moa-fe.vercel.app");  // Vercel 프론트엔드 (대체 도메인)
		config.addAllowedOrigin("https://kbmoa.store");     // 배포 백엔드 (필요시)
		
		// 또는 모든 Origin 허용 (개발 단계에서만 사용, 프로덕션에서는 특정 도메인만 허용)
		// config.addAllowedOriginPattern("*");
		
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.addExposedHeader("Authorization");
		config.addExposedHeader("Set-Cookie");
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
		// 한글 인코딩 필터 설정
		http
			.addFilterBefore(corsFilter(), ChannelProcessingFilter.class)
			.addFilterBefore(encodingFilter(), CsrfFilter.class)
			.addFilterBefore(authenticationErrorFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		http
			.cors().and()
			.exceptionHandling()
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler);

		http.httpBasic().disable()
			.csrf().disable()
			.formLogin().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

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
