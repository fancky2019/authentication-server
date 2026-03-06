package com.fancky.authorization.controller;


import com.fancky.authorization.entity.*;
import com.fancky.authorization.service.JwtService;
import com.fancky.authorization.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public Result<?> login() {
        return Result.error("请使用POST方式提交登录表单");
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("收到注册请求: {}", registerRequest.getUsername());

        boolean success = userService.register(registerRequest);

        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.error("注册失败");
        }
    }

    @PostMapping("/refresh")
    public Result<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("收到刷新令牌请求");

        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Result.error(400, "刷新令牌不能为空");
        }

        try {
            String newAccessToken = jwtService.refreshAccessToken(refreshToken);

            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("tokenType", jwtService.getTokenPrefix());
            result.put("expiresIn", String.valueOf(jwtService.getTokenExpiresIn(newAccessToken)));

            return Result.success(result);
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage());
            return Result.error(401, "刷新令牌失败: " + e.getMessage());
        }
    }

    /**
     * 登出的token 加入redis 黑名单
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = jwtService.getTokenFromRequest(request);

        if (token != null) {
            jwtService.invalidateToken(token);
            log.info("用户登出成功");
        }

        return Result.success("登出成功");
    }

//    @PreAuthorize("isAuthenticated()")  // 明确需要认证
    @GetMapping("/getCurrentUser")
    public Result<?> getCurrentUser(HttpServletRequest request) {
        String token = jwtService.getTokenFromRequest(request);

        if (token == null) {
            return Result.error(401, "未登录");
        }

        String username = jwtService.getUsernameFromToken(token);
        User user = userService.getUserWithRolesAndPermissions(username);

        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        return Result.success(userService.convertToVO(user));
    }

    @PostMapping("/changePassword")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                    HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("新密码与确认密码不一致");
        }

        String token = jwtService.getTokenFromRequest(httpRequest);
        String username = jwtService.getUsernameFromToken(token);

        try {
            boolean success = userService.changePassword(
                    username,
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                jwtService.invalidateToken(token);
                return Result.success("密码修改成功，请重新登录");
            } else {
                return Result.error("密码修改失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@RequestParam String email) {
        log.info("收到密码重置请求: {}", email);
        return Result.success("重置密码链接已发送到您的邮箱");
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("新密码与确认密码不一致");
        }

        try {
            boolean success = userService.resetPassword(
                    request.getUsername(),
                    request.getNewPassword()
            );

            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.error("密码重置失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public Result<?> verifyToken(HttpServletRequest request) {
        String token = jwtService.getTokenFromRequest(request);

        if (token == null) {
            return Result.error(401, "未提供令牌");
        }

        String username = jwtService.getUsernameFromToken(token);
        User user = userService.getUserWithRolesAndPermissions(username);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        boolean isValid = jwtService.validateToken(token, user);

        if (isValid) {
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("username", username);
            data.put("expiresIn", jwtService.getTokenExpiresIn(token));
            return Result.success(data);
        } else {
            return Result.error(401, "令牌无效或已过期");
        }
    }
}