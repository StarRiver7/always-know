package com.rag.business.controller;

import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import com.rag.business.dto.request.LoginRequest;
import com.rag.business.dto.request.RegisterRequest;
import com.rag.business.entity.User;
import com.rag.business.service.TokenService;
import com.rag.business.service.UserService;
import com.rag.business.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;



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

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            tokenService.removeToken(userId);
        }
        return Result.success(ResultCode.SUCCESS);
    }
}
