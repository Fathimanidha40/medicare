package com.medicare.controller;

import com.medicare.dto.ChangePasswordRequest;
import com.medicare.dto.UserProfileDto;
import com.medicare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(Authentication auth) {
        UserProfileDto profile = userService.getProfile(auth.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(Authentication auth, @RequestBody UserProfileDto dto) {
        UserProfileDto updated = userService.updateProfile(auth.getName(), dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(auth.getName(), req);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }
}
