package com.medicare.service;

import com.medicare.dto.ChangePasswordRequest;
import com.medicare.dto.UserProfileDto;
import com.medicare.entity.User;
import com.medicare.exception.BadRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        return mapToDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String email, UserProfileDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        user.setPhone(dto.getPhone());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setEmergencyContact(dto.getEmergencyContact());
        if (dto.getTimezone() != null && !dto.getTimezone().isBlank()) {
            user.setTimezone(dto.getTimezone());
        }

        User updated = userRepository.save(user);
        return mapToDto(updated);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        if (req.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    private UserProfileDto mapToDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getEmergencyContact(),
                user.getTimezone()
        );
    }
}
