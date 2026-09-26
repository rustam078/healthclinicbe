package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.UserDto;
import com.clinic.dto.ValidationGroups.OnLogin;
import com.clinic.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login and current user. Logout is handled by Spring Security at POST /api/auth/logout. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<UserDto> login(@Validated(OnLogin.class) @RequestBody UserDto credentials,
                                      HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.ok("Signed in", authService.login(credentials, request, response));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me() {
        return ApiResponse.ok(authService.me());
    }
}
