package com.rag.business.controller;

import com.rag.business.annotation.CurrentUserId;
import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import com.rag.business.dto.request.LoginRequest;
import com.rag.business.dto.request.RegisterRequest;
import com.rag.business.entity.User;
import com.rag.business.service.TokenService;
import com.rag.business.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;


    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = userService.login(request.getUsername(), request.getPassword());
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getRealName(),
                request.getEmail(),
                request.getPhone()
        );
        return Result.success(user);
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@CurrentUserId Long userId) {
        tokenService.removeToken(userId);
        return Result.success();
    }
}
