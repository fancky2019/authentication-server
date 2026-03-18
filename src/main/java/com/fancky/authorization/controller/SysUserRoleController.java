package com.fancky.authorization.controller;


import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户角色关联控制器
 */
@RestController
@RequestMapping("/api/user-role")
public class SysUserRoleController {

    @Autowired
    private SysUserRoleService userRoleService;

    /**
     * 获取用户的角色ID列表
     */
    @GetMapping("/user/{userId}/role-ids")
    public MessageResult<List<Long>> getRoleIdsByUserId(@PathVariable Long userId) {
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        return MessageResult.success(roleIds);
    }

    /**
     * 获取角色的用户ID列表
     */
    @GetMapping("/role/{roleId}/user-ids")
    public MessageResult<List<Long>> getUserIdsByRoleId(@PathVariable Long roleId) {
        List<Long> userIds = userRoleService.getUserIdsByRoleId(roleId);
        return MessageResult.success(userIds);
    }

    /**
     * 为用户分配角色(新增、编辑)
     */
    @PostMapping("/assign-to-user")
    public MessageResult<String> assignRolesToUser(@RequestParam Long userId, @RequestBody List<Long> roleIds) throws Exception {
        userRoleService.assignRolesToUser(userId, roleIds);
        return MessageResult.success("角色分配成功");
    }

//    /**
//     * 为角色分配用户
//     */
//    @PostMapping("/assign-to-role")
//    public MessageResult<String> assignUsersToRole(@RequestParam Long roleId, @RequestBody List<Long> userIds) {
//        userRoleService.assignUsersToRole(roleId, userIds);
//        return MessageResult.success("用户分配成功");
//    }

    /**
     * 移除用户的某个角色
     */
    @DeleteMapping("/remove")
    public MessageResult<String> removeUserRole(@RequestParam Long userId, @RequestParam Long roleId) {
        userRoleService.removeUserRole(userId, roleId);
        return MessageResult.success("角色移除成功");
    }

    /**
     * 移除用户的所有角色
     */
    @DeleteMapping("/user/{userId}/remove-all")
    public MessageResult<String> removeUserRoles(@PathVariable Long userId) {
        userRoleService.removeUserRoles(userId);
        return MessageResult.success("所有角色移除成功");
    }

    /**
     * 移除角色的所有用户
     */
    @DeleteMapping("/role/{roleId}/remove-all")
    public MessageResult<String> removeRoleUsers(@PathVariable Long roleId) throws Exception {
        userRoleService.removeByRole(roleId);
        return MessageResult.success("所有用户移除成功");
    }

    /**
     * 检查用户是否拥有某个角色
     */
    @GetMapping("/check")
    public MessageResult<Boolean> checkUserRole(@RequestParam Long userId, @RequestParam Long roleId) {
        boolean hasRole = userRoleService.hasRole(userId, roleId);
        return MessageResult.success(hasRole);
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