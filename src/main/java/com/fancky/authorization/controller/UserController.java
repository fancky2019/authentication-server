package com.fancky.authorization.controller;

import com.fancky.authorization.model.dto.CheckPermissionDto;
import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.request.ChangePasswordRequest;
import com.fancky.authorization.model.request.RefreshTokenRequest;
import com.fancky.authorization.model.request.RegisterRequest;
import com.fancky.authorization.model.request.ResetPasswordRequest;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.JwtService;
import com.fancky.authorization.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserService userService;


    @Autowired
    private JwtService jwtService;

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
        boolean success = userService.register(registerRequest);
        if (success) {
            return MessageResult.success("注册成功");
        } else {
            return MessageResult.faile("注册失败");
        }
    }

    @PostMapping("/refresh")
    public MessageResult<?> refreshToken(@RequestBody RefreshTokenRequest request) throws Exception {
        return MessageResult.success(userService.refreshToken(request));
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
        }

        return MessageResult.success("登出成功");
    }

    //    @PreAuthorize("isAuthenticated()")  // 明确需要认证
    @GetMapping("/current-user")
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

        return MessageResult.success(user);
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

    /**
     *
     *
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public MessageResult<PageVO<SysUser>> page(UserDTO userDTO) {
        PageVO<SysUser> page = userService.getUserPage(userDTO);
        return MessageResult.success(page);
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public MessageResult<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        return MessageResult.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    public MessageResult<Void> add(@Valid @RequestBody UserDTO userDTO) {
        userService.addUser(userDTO);
        return MessageResult.success();
    }

    /**
     * 修改用户
     */
    @PutMapping
    public MessageResult<Void> update(@Valid @RequestBody UserDTO userDTO) {
        userService.updateUser(userDTO);
        return MessageResult.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public MessageResult<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return MessageResult.success();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public MessageResult<Void> deleteBatch(@RequestBody Long[] ids) {
        userService.deleteBatch(ids);
        return MessageResult.success();
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/status")
    public MessageResult<Void> updateStatus(@RequestParam Long id, @RequestParam Integer enabled) {
        userService.updateStatus(id, enabled);
        return MessageResult.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password")
    public MessageResult<Void> resetPassword(@RequestParam Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return MessageResult.success();
    }

    /**
     * 分配角色
     */
    @PostMapping("/assign-roles")
    public MessageResult<Void> assignRoles(@RequestParam Long userId, @RequestBody Long[] roleIds) {
        userService.assignRoles(userId, roleIds);
        return MessageResult.success();
    }


    //region  add

    @PostMapping("/user-name")
    public MessageResult<String> getUsername(HttpServletRequest request) {
        return MessageResult.success(jwtService.getUsername(request));
    }

    @PostMapping("/check-permission")
    public MessageResult<CheckPermissionDto> checkPermission(@RequestBody CheckPermissionDto dto, HttpServletRequest request) throws Exception {
        return MessageResult.success(userService.checkPermission(dto, request));
    }


    //endregion
}