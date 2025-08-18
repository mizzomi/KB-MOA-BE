package org.moa.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import java.nio.charset.StandardCharsets;
import java.util.List;

@EnableWebMvc
@ComponentScan(basePackages = {
	"org.moa.global.controller",
	"org.moa.global.handler",  // GlobalExceptionHandler
	"org.moa.member.controller",
	"org.moa.trip.controller",
	"org.moa.reservation.controller",
	"org.moa.reservation.transport.controller",
	"org.moa.reservation.accommodation.controller",
	"org.moa.reservation.restaurant.controller",
	"org.moa.global.security.controller",
	"org.moa.global.notification.controller",
	"org.moa.global.account.controller"
})
public class ServletConfig implements WebMvcConfigurer {

	@Value("${app.environment:local}")
	private String environment;

	/**
	 * CORS 설정 - RTR을 위한 Cookie 전송 허용
	 */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if ("docker".equals(environment) || "production".equals(environment)) {
			// 배포 환경 - HTTPS
			registry.addMapping("/api/**")
				.allowedOrigins("https://yourdomain.com", "https://www.yourdomain.com")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true)  // Cookie 전송 허용 (RTR 필수)
				.maxAge(3600);
		} else {
			// 로컬 환경 - HTTP
			registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true)  // Cookie 전송 허용 (RTR 필수)
				.maxAge(3600);
		}
	}

	// Controller에서 메소드 파라미터로 Pageable 타입을 사용할 수 있도록 설정
	// Spring이 ?page=...&size=... 같은 파라미터를 Pageable 객체로 변환할 수 있도록
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new PageableHandlerMethodArgumentResolver());
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/resources/**")     // url이 /resources/로 시작하는 모든 경로
			.addResourceLocations("/resources/");    // webapp/resources/경로로 매핑
	}

	// jsp view resolver 설정
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		InternalResourceViewResolver bean = new InternalResourceViewResolver();

		bean.setViewClass(JstlView.class);
		bean.setPrefix("/WEB-INF/views/");
		bean.setSuffix(".jsp");

		registry.viewResolver(bean);
	}

	// HTTP 메시지 컨버터 설정 - UTF-8 인코딩 강제
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// String 메시지 컨버터 - UTF-8 설정
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
		converters.add(stringConverter);
		
		// JSON 메시지 컨버터 - UTF-8 설정
		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
		jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
		converters.add(jsonConverter);
	}

	//	Servlet 3.0 파일 업로드 사용시
	@Bean
	public MultipartResolver multipartResolver() {
		StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
		return resolver;
	}
}
