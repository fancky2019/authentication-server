package com.fancky.authorization.controller;


import com.fancky.authorization.model.entity.*;
import com.fancky.authorization.model.request.ChangePasswordRequest;
import com.fancky.authorization.model.request.RefreshTokenRequest;
import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.request.ResetPasswordRequest;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.service.JwtService;
import com.fancky.authorization.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final SysUserService userService;
    private final JwtService jwtService;

    /**
     * 登录实现在LoginFilter
     * @return
     */
    @PostMapping("/login")
    public MessageResult<?> login() {
        return MessageResult.faile("login fail");
    }

    @PostMapping("/register")
    public MessageResult<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("收到注册请求: {}", registerRequest.getUsername());
        boolean success = userService.register(registerRequest);
        if (success) {
            return MessageResult.success("注册成功");
        } else {
            return MessageResult.faile("注册失败");
        }
    }

    @PostMapping("/refresh")
    public MessageResult<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("收到刷新令牌请求");

        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return MessageResult.faile(400, "刷新令牌不能为空");
        }

        try {
            String newAccessToken = jwtService.refreshAccessToken(refreshToken);

            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("tokenType", jwtService.getTokenPrefix());
            result.put("expiresIn", String.valueOf(jwtService.getTokenExpiresIn(newAccessToken)));

            return MessageResult.success(result);
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage());
            return MessageResult.faile(401, "刷新令牌失败: " + e.getMessage());
        }
    }



    /**
     * 登出的token 加入redis 黑名单
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public MessageResult<?> logout(HttpServletRequest request) {
        String token = jwtService.getTokenFromRequest(request);

        if (token != null) {
            jwtService.invalidateToken(token);
            log.info("用户登出成功");
        }

        return MessageResult.success("登出成功");
    }

//    @PreAuthorize("isAuthenticated()")  // 明确需要认证
    @GetMapping("/getCurrentUser")
    public MessageResult<?> getCurrentUser(HttpServletRequest request) throws Exception {
        String token = jwtService.getTokenFromRequest(request);

        if (token == null) {
            return MessageResult.faile(401, "未登录");
        }

        String username = jwtService.getUsernameFromToken(token);
        SysUser user = userService.getUserWithRolesAndPermissions(username);

        if (user == null) {
            return MessageResult.faile(404, "用户不存在");
        }

        return MessageResult.success(userService.convertToVO(user));
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString();
            }
        } catch (Exception e) {
            return "system";
        }
    }

    @PostMapping("/changePassword")
    public MessageResult<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                           HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return MessageResult.faile("新密码与确认密码不一致");
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
                return MessageResult.success("密码修改成功，请重新登录");
            } else {
                return MessageResult.faile("密码修改失败");
            }
        } catch (Exception e) {
            return MessageResult.faile(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public MessageResult<?> forgotPassword(@RequestParam String email) {
        log.info("收到密码重置请求: {}", email);
        return MessageResult.success("重置密码链接已发送到您的邮箱");
    }

    @PostMapping("/reset-password")
    public MessageResult<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return MessageResult.faile("新密码与确认密码不一致");
        }

        try {
            boolean success = userService.resetPassword(
                    request.getUsername(),
                    request.getNewPassword()
            );

            if (success) {
                return MessageResult.success("密码重置成功");
            } else {
                return MessageResult.faile("密码重置失败");
            }
        } catch (Exception e) {
            return MessageResult.faile(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public MessageResult<?> verifyToken(HttpServletRequest request) throws Exception {
        String token = jwtService.getTokenFromRequest(request);

        if (token == null) {
            return MessageResult.faile(401, "未提供令牌");
        }

        String username = jwtService.getUsernameFromToken(token);
        SysUser user = userService.getUserWithRolesAndPermissions(username);

        if (user == null) {
            return MessageResult.faile(401, "用户不存在");
        }

        boolean isValid = jwtService.validateToken(token, user);

        if (isValid) {
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("username", username);
            data.put("expiresIn", jwtService.getTokenExpiresIn(token));
            return MessageResult.success(data);
        } else {
            return MessageResult.faile(401, "令牌无效或已过期");
        }
    }
}