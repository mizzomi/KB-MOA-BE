# Production Environment Setup Guide

## Current Settings (Development)
- **HTTP Only** (No HTTPS)
- **Cookies**: Not secure (works with HTTP)
- **Rate Limiting**: Relaxed (100 req/sec)
- **Database**: Local MySQL

## Production Changes Required

### 1. application.properties Override
Create a new file or use JVM arguments:

```properties
# SSL/HTTPS (Required for Production)
server.ssl.enabled=true
cookie.secure=true

# Database (Production Server)
jdbc.url=jdbc:mysql://prod-server:3306/moa?useSSL=true&serverTimezone=Asia/Seoul
jdbc.username=prod_user
jdbc.password=${DB_PASSWORD}

# JWT Secret (Must be different from development)
jwt.secret=${JWT_SECRET_PROD}

# Cookie Domain (Your production domain)
jwt.refresh.cookie.domain=yourdomain.com

# Rate Limiting (Stricter for production)
rate.limit.requests-per-second=10
rate.limit.burst-capacity=20

# Brute Force Protection (Stricter)
security.brute-force.max-attempts=5
security.brute-force.lockout-duration=5

# Environment Flag
environment=production
```

### 2. SSL Certificate Setup (for Tomcat)
```xml
<!-- server.xml -->
<Connector port="443" protocol="HTTP/1.1"
           SSLEnabled="true"
           scheme="https"
           secure="true"
           keystoreFile="/path/to/keystore.jks"
           keystorePass="your-password"
           clientAuth="false"
           sslProtocol="TLS"/>
```

### 3. CORS Configuration Update
In SecurityConfig.java, change:
```java
config.addAllowedOrigin("https://yourdomain.com");  // Not localhost
```

### 4. Deployment Command
```bash
# Set environment variables
export DB_PASSWORD=your_prod_password
export JWT_SECRET_PROD=your_prod_secret_min_256_bits

# Deploy with override properties
java -jar app.war \
  -Dspring.config.location=classpath:/application.properties,/config/application-prod.properties
```

## Security Checklist for Production

- [ ] HTTPS enabled (SSL certificate installed)
- [ ] Secure cookies enabled (`cookie.secure=true`)
- [ ] JWT secret changed from development
- [ ] Database credentials secured (use environment variables)
- [ ] Rate limiting configured appropriately
- [ ] CORS origins updated to production domain
- [ ] Logging level set to WARN or ERROR
- [ ] Security audit logs enabled and monitored
- [ ] Firewall rules configured (only 443 open)
- [ ] Remove development endpoints (/api/admin/security/* behind auth)
