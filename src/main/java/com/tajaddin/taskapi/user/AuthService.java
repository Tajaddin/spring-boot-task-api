package com.tajaddin.taskapi.user;

import com.tajaddin.taskapi.security.JwtService;
import com.tajaddin.taskapi.user.dto.AuthResponse;
import com.tajaddin.taskapi.user.dto.LoginRequest;
import com.tajaddin.taskapi.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }
        User user = users.save(new User(email, encoder.encode(request.password())));
        return AuthResponse.bearer(jwt.issue(user.getId(), user.getEmail()), jwt.getTtlSeconds());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid email or password"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid email or password");
        }
        return AuthResponse.bearer(jwt.issue(user.getId(), user.getEmail()), jwt.getTtlSeconds());
    }
}
