package com.fancky.authorization.controller;


import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户角色关联控制器
 */
@RestController
@RequestMapping("/user-role")
public class SysUserRoleController {

    @Autowired
    private SysUserRoleService userRoleService;

    /**
     * 获取用户的角色ID列表
     */
    @GetMapping("/user/{userId}/role-ids")
    public Result<List<Long>> getRoleIdsByUserId(@PathVariable Long userId) {
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        return Result.success(roleIds);
    }

    /**
     * 获取角色的用户ID列表
     */
    @GetMapping("/role/{roleId}/user-ids")
    public Result<List<Long>> getUserIdsByRoleId(@PathVariable Long roleId) {
        List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
        return Result.success(userIds);
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/assign-to-user")
    public Result<String> assignRolesToUser(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        userRoleService.assignRolesToUser(userId, roleIds);
        return Result.success("角色分配成功");
    }

    /**
     * 为角色分配用户
     */
    @PostMapping("/assign-to-role")
    public Result<String> assignUsersToRole(@RequestParam Long roleId, @RequestBody List<Long> userIds) {
        userRoleService.assignUsersToRole(roleId, userIds);
        return Result.success("用户分配成功");
    }

    /**
     * 移除用户的某个角色
     */
    @DeleteMapping("/remove")
    public Result<String> removeUserRole(@RequestParam Long userId, @RequestParam Long roleId) {
        userRoleService.removeUserRole(userId, roleId);
        return Result.success("角色移除成功");
    }

    /**
     * 移除用户的所有角色
     */
    @DeleteMapping("/user/{userId}/remove-all")
    public Result<String> removeUserRoles(@PathVariable Long userId) {
        userRoleService.removeUserRoles(userId);
        return Result.success("所有角色移除成功");
    }

    /**
     * 移除角色的所有用户
     */
    @DeleteMapping("/role/{roleId}/remove-all")
    public Result<String> removeRoleUsers(@PathVariable Long roleId) {
        userRoleService.removeRoleUsers(roleId);
        return Result.success("所有用户移除成功");
    }

    /**
     * 检查用户是否拥有某个角色
     */
    @GetMapping("/check")
    public Result<Boolean> checkUserRole(@RequestParam Long userId, @RequestParam Long roleId) {
        boolean hasRole = userRoleService.hasRole(userId, roleId);
        return Result.success(hasRole);
    }

//    /**
//     * 批量检查用户角色
//     */
//    @PostMapping("/check-batch")
//    @PreAuthorize("hasAuthority('system:user:query')")
//    public Result<List<Long>> checkUserRoles(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
//        List<Long> existingRoleIds = userRoleService.getExistingRoleIds(userId, roleIds);
//        return Result.success(existingRoleIds);
//    }
}