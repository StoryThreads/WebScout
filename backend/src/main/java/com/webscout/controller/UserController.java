package com.webscout.controller;

import com.webscout.dto.UserResponse;
import com.webscout.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        UserResponse response = userService.getCurrentUser(userId);

        return ResponseEntity.ok(response);
    }
}