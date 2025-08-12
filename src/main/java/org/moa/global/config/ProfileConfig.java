package org.moa.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;

/**
 * Spring 프로필 설정 클래스
 * Docker 환경에서는 application-docker.properties를 사용
 */
@Configuration
public class ProfileConfig {
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void init() {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = env.getDefaultProfiles();
        }
        
        System.out.println("========================================");
        System.out.println("Active Profiles: " + String.join(", ", profiles));
        System.out.println("========================================");
        
        // Docker 환경인지 확인
        for (String profile : profiles) {
            if ("docker".equals(profile)) {
                System.out.println("Docker Profile Activated!");
                System.out.println("Using Docker network hostnames (mysql, redis)");
                break;
            }
        }
    }
}
