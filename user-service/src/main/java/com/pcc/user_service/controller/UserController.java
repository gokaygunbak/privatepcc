package com.pcc.user_service.controller;

import com.pcc.user_service.model.UserProfile;
import com.pcc.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
// @RequiredArgsConstructor
// @CrossOrigin("*")
public class UserController {

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private final UserService userService;

    // Profil getir
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable Long userId) {
        UserProfile profile = userService.getProfile(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    // Profil güncelle
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable Long userId, @RequestBody UserProfile profile) {
        System.out.println("Update Profile Request Received for User ID: " + userId);
        return ResponseEntity.ok(userService.updateProfile(userId, profile));
    }
}