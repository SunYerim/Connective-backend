package com.connective.server.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (테스트 목적, 실제 서비스에서는 보안 고려)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/**").permitAll() // /auth로 시작하는 모든 요청은 인증 없이 허용
                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
            )
            .formLogin(formLogin -> formLogin.disable()) // 기본 폼 로그인 비활성화
            .httpBasic(httpBasic -> httpBasic.disable()); // 기본 HTTP Basic 인증 비활성화

        return http.build();
    }

}
