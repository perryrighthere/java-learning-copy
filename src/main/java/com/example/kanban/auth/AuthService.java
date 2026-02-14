package com.example.kanban.auth;

import com.example.kanban.domain.User;
import com.example.kanban.repository.UserRepository;
import com.example.kanban.web.dto.LoginRequest;
import com.example.kanban.web.dto.TokenPairResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenPairResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokenPair(user);
    }

    public TokenPairResponse refresh(String refreshToken) {
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parseAndValidate(refreshToken, TokenType.REFRESH);

        User user = userRepository.findById(claims.userId())
            .orElseThrow(() -> new BadCredentialsException("Refresh token is no longer valid"));

        return issueTokenPair(user);
    }

    private TokenPairResponse issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
        String refreshToken = jwtTokenProvider.createToken(user, TokenType.REFRESH);

        return new TokenPairResponse("Bearer", accessToken, jwtTokenProvider.accessTokenExpiresInSeconds(), refreshToken);
    }
}
