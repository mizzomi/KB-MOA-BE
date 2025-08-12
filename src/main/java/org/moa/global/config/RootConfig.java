package org.moa.global.config;

import java.util.Properties;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@MapperScan(basePackages = {"org.moa.global.account.mapper", "org.moa.member.mapper",
		"org.moa.trip.mapper", "org.moa.reservation.transport.mapper",
		"org.moa.reservation.mapper", "org.moa.reservation.accommodation.mapper",
		"org.moa.reservation.restaurant.mapper", "org.moa.global.notification.mapper"})
@ComponentScan(basePackages = {"org.moa"})
@Import(RedisConfig.class)  // Redis 설정 추가
public class RootConfig {
	
	/**
	 * 기본 설정 (로컬 환경)
	 */
	@Configuration
	@Profile("!docker")  // docker 프로필이 아닐 때 활성화
	@PropertySource({"classpath:/application.properties"})
	static class DefaultConfig {
		@Autowired
		ApplicationContext applicationContext;

		@Value("${jdbc.driver}")
		String driver;
		@Value("${jdbc.url}")
		String url;
		@Value("${jdbc.username}")
		String username;
		@Value("${jdbc.password}")
		String password;

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		public DataSource dataSource() {
			HikariConfig config = new HikariConfig();

			config.setDriverClassName(driver);
			config.setJdbcUrl(url);
			config.setUsername(username);
			config.setPassword(password);
			
			// 로컬 환경 설정
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(5);
			config.setConnectionTimeout(30000);
			
			// UTF-8 인코딩 설정 추가
			Properties props = new Properties();
			props.setProperty("useUnicode", "true");
			props.setProperty("characterEncoding", "UTF-8");
			props.setProperty("connectionCollation", "utf8mb4_unicode_ci");
			config.setDataSourceProperties(props);

			System.out.println("========================================");
			System.out.println("Using Local Configuration");
			System.out.println("Database URL: " + url);
			System.out.println("========================================");

			HikariDataSource dataSource = new HikariDataSource(config);
			return dataSource;
		}

		@Bean
		public SqlSessionFactory sqlSessionFactory() throws Exception {
			SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
			sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
			sqlSessionFactory.setDataSource(dataSource());
			sqlSessionFactory.setMapperLocations(applicationContext.getResources("classpath:/org/moa/**/*.xml"));
			
			return sqlSessionFactory.getObject();
		}

		@Bean
		public DataSourceTransactionManager transactionManager() {
			DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
			return manager;
		}
	}
	
	/**
	 * Docker 환경 설정
	 */
	@Configuration
	@Profile("docker")  // docker 프로필일 때만 활성화
	@PropertySource({"classpath:/application-docker.properties"})
	static class DockerConfig {
		@Autowired
		ApplicationContext applicationContext;

		@Value("${jdbc.driver}")
		String driver;
		@Value("${jdbc.url}")
		String url;
		@Value("${jdbc.username}")
		String username;
		@Value("${jdbc.password}")
		String password;

		@Bean
		public DataSource dataSource() {
			HikariConfig config = new HikariConfig();

			config.setDriverClassName(driver);
			config.setJdbcUrl(url);
			config.setUsername(username);
			config.setPassword(password);
			
			// Docker 환경 설정 (컨테이너 환경에 맞게 조정)
			config.setMaximumPoolSize(20);
			config.setMinimumIdle(10);
			config.setConnectionTimeout(60000);  // Docker 네트워크는 시간이 더 걸릴 수 있음
			
			// UTF-8 인코딩 설정 추가
			Properties props = new Properties();
			props.setProperty("useUnicode", "true");
			props.setProperty("characterEncoding", "UTF-8");
			props.setProperty("connectionCollation", "utf8mb4_unicode_ci");
			config.setDataSourceProperties(props);

			System.out.println("========================================");
			System.out.println("Using Docker Configuration");
			System.out.println("Database URL: " + url);
			System.out.println("========================================");

			HikariDataSource dataSource = new HikariDataSource(config);
			return dataSource;
		}

		@Bean
		public SqlSessionFactory sqlSessionFactory() throws Exception {
			SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
			sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
			sqlSessionFactory.setDataSource(dataSource());
			sqlSessionFactory.setMapperLocations(applicationContext.getResources("classpath:/org/moa/**/*.xml"));
			
			return sqlSessionFactory.getObject();
		}

		@Bean
		public DataSourceTransactionManager transactionManager() {
			DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());
			return manager;
		}
	}
}
