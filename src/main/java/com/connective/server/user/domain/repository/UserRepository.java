package com.connective.server.user.domain.repository;

import com.connective.server.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.stereotype.Repository;

@Repository
@EnableRedisRepositories
public interface UserRepository extends JpaRepository<User, Long> {

}
