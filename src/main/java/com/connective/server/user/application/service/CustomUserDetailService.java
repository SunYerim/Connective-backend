package com.connective.server.user.application.service;

import com.connective.server.user.domain.entity.User;
import com.connective.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userIdString) throws UsernameNotFoundException {
        Long userId = Long.valueOf(userIdString);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        // 모든 사용자가 'ROLE_USER' 권한을 가지도록 설정함
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getId().toString())
            .password("")
            .authorities("ROLE_USER")
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();

    }
}
