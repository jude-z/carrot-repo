package jude.carrot.web.config;


import jude.carrot.web.auth.JsonAuthenticationFailureHandler;
import jude.carrot.web.auth.JsonAuthenticationSuccessHandler;
import jude.carrot.web.auth.JsonLogoutSuccessHandler;
import jude.carrot.web.auth.UserAuthenticationProvider;
import jude.carrot.web.filter.JsonAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.GET;



@Configuration
public class SecurityConfig {

    private final String logoutUrl;
    private final JsonAuthenticationFilter jsonAuthenticationFilter;
    public SecurityConfig(@Value("${jude.auth.default-url}") String defaultUrl,
                          @Value("${jude.auth.logout-url}") String logoutUrl,
                          JsonAuthenticationFilter jsonAuthenticationFilter
                          ) {
        this.logoutUrl = defaultUrl + logoutUrl;
        this.jsonAuthenticationFilter = jsonAuthenticationFilter;
    }

    @Bean
    public static AuthenticationManager authenticationManager(UserAuthenticationProvider userAuthenticationProvider){
        return new ProviderManager(userAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonAuthenticationFailureHandler jsonAuthenticationFailureHandler,
                                                   JsonAuthenticationSuccessHandler jsonAuthenticationSuccessHandler,
                                                   JsonLogoutSuccessHandler jsonLogoutSuccessHandler){
        jsonAuthenticationFilter.setAuthenticationFailureHandler(jsonAuthenticationFailureHandler);
        jsonAuthenticationFilter.setAuthenticationSuccessHandler(jsonAuthenticationSuccessHandler);
        jsonAuthenticationFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        String[] getWhiteList = Arrays.stream(WhiteList.values())
                .filter(url -> url.getMethod().equals("GET"))
                .map(url -> url.getUrl())
                        .toArray(String[]::new);

        String[] postWhiteList = Arrays.stream(WhiteList.values())
                .filter(url -> url.getMethod().equals("POST"))
                .map(url -> url.getUrl())
                .toArray(String[]::new);
        http
                .csrf(CsrfConfigurer::disable)
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource()))
                .httpBasic(HttpBasicConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(GET,getWhiteList).permitAll()
                        .requestMatchers(POST,postWhiteList).permitAll()
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutUrl(logoutUrl)
                        .logoutSuccessHandler(jsonLogoutSuccessHandler));
        return http.build();
    }
    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedOrigin("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);
        return source;
    }
}
