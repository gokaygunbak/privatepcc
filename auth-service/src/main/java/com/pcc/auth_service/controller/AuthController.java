package com.pcc.auth_service.controller;

import com.pcc.auth_service.model.User;
import com.pcc.auth_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String token = authService.login(username, loginRequest.get("password"));

        if (token != null) {
            // Token ve User ID'yi dön
            User user = authService.getUserByUsername(username).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userId", user.getId()));
        }
        return ResponseEntity.status(401).body("Hatalı kullanıcı adı veya şifre!");
    }
}