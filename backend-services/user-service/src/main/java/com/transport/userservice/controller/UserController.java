package com.transport.userservice.controller;

import com.transport.userservice.dto.UserResponse;
import com.transport.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // Constructor manuel
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}