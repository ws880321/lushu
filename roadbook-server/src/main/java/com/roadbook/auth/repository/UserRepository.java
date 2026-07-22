package com.roadbook.auth.repository;

import com.roadbook.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOpenid(String openid);
    Optional<User> findByPhone(String phone);
}
