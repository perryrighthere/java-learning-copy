package com.example.kanban.auth;

import com.example.kanban.config.JwtProperties;
import com.example.kanban.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String createToken(User user, TokenType tokenType) {
        Instant now = Instant.now();
        Duration ttl = tokenType == TokenType.ACCESS ? jwtProperties.accessTtl() : jwtProperties.refreshTtl();

        return Jwts.builder()
            .subject(user.getId().toString())
            .issuer(jwtProperties.issuer())
            .claim("email", user.getEmail())
            .claim("token_type", tokenType.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(signingKey())
            .compact();
    }

    public TokenClaims parseAndValidate(String token, TokenType expectedType) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        TokenType actualType = TokenType.valueOf(claims.get("token_type", String.class));
        if (actualType != expectedType) {
            throw new JwtException("Invalid token type");
        }

        return new TokenClaims(
            Long.valueOf(claims.getSubject()),
            claims.get("email", String.class),
            actualType
        );
    }

    public long accessTokenExpiresInSeconds() {
        return jwtProperties.accessTtl().toSeconds();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public record TokenClaims(Long userId, String email, TokenType tokenType) {
    }
}
