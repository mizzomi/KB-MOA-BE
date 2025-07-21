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
@MapperScan(basePackages = {"org.moa.member.mapper", "org.moa.global.mapper"})
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

	//BCryptPasswordEncoder 빈 등록
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // 보안 설정의 일환
	}

	//AuthenticationManager 빈 등록
	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManager();
	}

	//cross origin 접근 허용 (백엔드 서버와 다른 origin에서의 접근 허용) => ex) 백엔드서버(로컬) - 프론트엔드(browser)
	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOriginPattern("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	// 접근 제한 무시 경로 설정 – resource
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/assets/**", "/*");
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
		//한글 인코딩 필터 설정
		http.addFilterBefore(encodingFilter(), CsrfFilter.class)
			//인증 에러 필터
			.addFilterBefore(authenticationErrorFilter, UsernamePasswordAuthenticationFilter.class)
			//Jwt 인증 필터
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			//로그인 인증 필터
			.addFilterBefore(jwtUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		http
			.exceptionHandling()
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler);

		http.httpBasic().disable() // 기본 HTTP 인증 비활성화
			.csrf().disable() // CSRF 비활성화
			.formLogin().disable() // formLogin 비활성화  관련 필터 해제
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 세션 생성 모드 설정

		http
			.authorizeRequests() // 경로별 접근 권한 설정
			.antMatchers(HttpMethod.OPTIONS)
			.permitAll()
			.antMatchers("/api/public/**")
			.permitAll()
			.antMatchers("/api/member/**")
			.permitAll() // 모두 허용
			.antMatchers("/api/member")
			.access("hasRole('ROLE_USER')")
			.antMatchers("/api/admin")
			.access("hasRole('ROLE_ADMIN')")
			.antMatchers("/api/owner/**")
			.access("hasRole('ROLE_OWNER')")
			.anyRequest()
			.authenticated(); // 나머지는 로그인 된 경우 모두 허용
	}

	//Authentication Manager 구성
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
			.userDetailsService(userDetailsService)
			.passwordEncoder(passwordEncoder()); // 여기까지가 DB 인증 방식
	}
}
