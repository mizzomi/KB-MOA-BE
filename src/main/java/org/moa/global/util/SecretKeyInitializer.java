package org.moa.global.util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SecretKeyInitializer {

    @Value("${my.security.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        AesUtil.setSecretKey(secretKey);
    }
}
