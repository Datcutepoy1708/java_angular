package com.store.repository;

import com.store.entity.auth.AuthToken;
import com.store.entity.auth.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenAndTokenType(String token, TokenType tokenType);

    @Modifying
    void deleteByUser_UserIdAndTokenType(Long userId, TokenType tokenType);

    @Modifying
    void deleteByToken(String token);
}
