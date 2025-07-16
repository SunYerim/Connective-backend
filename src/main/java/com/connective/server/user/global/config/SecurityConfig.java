package com.connective.server.user.global.config;

import com.connective.server.user.infrastructure.security.JwtAccessDeniedHandler;
import com.connective.server.user.infrastructure.security.JwtAuthenticationEntryPoint;
import com.connective.server.user.infrastructure.security.JwtAuthenticationFilter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // // CSRF 보호 비활성화

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .formLogin(formLogin -> formLogin.disable()) // 기본 폼 로그인 비활성화
            .httpBasic(httpBasic -> httpBasic.disable()) // 기본 HTTP Basic 인증 비활성화

            // 세션을 사용하지 않도록 stateless 설정
            .sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 예외 처리 핸들러 등록
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
            )

            // HTTP 요청에 대한 인가 규칙 설정
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    // 인증이 필요 없는 경로 설정 (permitAll)
                    // - /auth/**: Google 로그인 관련 (콜백 포함)
                    // - /api/auth/reissue: Refresh Token 재발급 (나중에 구현할 예정)
                    // - /error: Spring Boot 기본 에러 페이지
                    .requestMatchers(
                        "/auth/**",
                        "/api/auth/reissue", // Refresh Token 재발급 엔드포인트 (구현 예정)
                        "/error",
                        "/swagger-ui/**"
                    ).permitAll()

                    // 나머지 모든 요청은 인증된 사용자만 허용
                    .anyRequest().authenticated()
            );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정 Bean
    // Nginx & server 모두 cors 허용 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
            Arrays.asList("https://v0-simple-toy-project.vercel.app"));

        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정 적용
        return source;
    }

}
