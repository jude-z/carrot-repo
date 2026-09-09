package jude.carrot.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * web 모듈(Security, 세션, 필터, 핸들러, KakaoClient 등)을 의존하는 서버에 자동 등록한다.
 *
 * before: 이 모듈이 직접 정의하는 빈을 @ConditionalOnMissingBean 으로 양보하는 Boot 자동설정.
 *  - ServletWebSecurityAutoConfiguration : SecurityConfig 의 SecurityFilterChain
 *  - UserDetailsServiceAutoConfiguration : SecurityConfig 의 AuthenticationManager / UserAuthenticationProvider
 *  - SessionDataRedisAutoConfiguration   : RedisSessionConfig 의 @EnableRedisHttpSession (SessionRepository)
 */
@AutoConfiguration(before = {
        ServletWebSecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        SessionDataRedisAutoConfiguration.class
})
@ComponentScan(
        basePackages = "jude.carrot.web",
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
)
public class WebAutoConfiguration {
}
