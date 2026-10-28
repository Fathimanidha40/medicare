package com.medicare.service;

import com.medicare.dto.AuthResponse;
import com.medicare.dto.LoginRequest;
import com.medicare.dto.RegisterRequest;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.repository.UserRepository;
import com.medicare.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("An account with email " + req.getEmail() + " already exists");
        }

        User user = new User(
                req.getName(),
                req.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(req.getPassword()),
                req.getPhone(),
                req.getDateOfBirth(),
                req.getEmergencyContact(),
                req.getTimezone()
        );

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getTimezone()
        );
    }

    public AuthResponse login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase().trim(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtUtils.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getTimezone()
        );
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Authenticated user not found"));
    }
}
