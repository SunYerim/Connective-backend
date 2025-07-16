package com.connective.server.user.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 accessToken 추출
        String accessToken = resolveToken(request);

        // 2. accessToken 유효성 검증
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            // 3. 토큰이 유효하면 인증정보 설정
            try {
                // claim 추출
                Claims claims = jwtTokenProvider.parseClaims(accessToken);

                String userIdString = claims.getSubject();
                Long userId = Long.valueOf(userIdString);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId, // Principal (사용자 식별자)
                    null,   // Credentials (비밀번호 등, JWT에서는 필요 없음)
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                    // 권한 (예: USER, ADMIN)
                );

                // securityContextHolder에 Authentication 객체 설정
                // 현재 요청에 대한 사용자가 인증되었음을 Spring Security에 알림
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authenticated user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to set user authentication in security context: {}",
                    e.getMessage());
            }
        } else {
            log.debug("No valid JWT token found for request: {}", request.getRequestURI());
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 Access Token을 추출합니다. "Bearer <token>" 형식에서 <token> 부분만 가져옵니다.
     *
     * @param request HttpServletRequest
     * @return Access Token 문자열 또는 null
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
