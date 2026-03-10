package com.fancky.authorization.controller;



import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private  SysUserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<PageVO<SysUser>> page(UserDTO userDTO) {
        PageVO<SysUser> page = userService.getUserPage(userDTO);
        return Result.success(page);
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserDTO userDTO) {
        userService.addUser(userDTO);
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserDTO userDTO) {
        userService.updateUser(userDTO);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody Long[] ids) {
        userService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer enabled) {
        userService.updateStatus(id, enabled);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.success();
    }

    /**
     * 分配角色
     */
    @PostMapping("/assign-roles")
    public Result<Void> assignRoles(@RequestParam Long userId, @RequestBody Long[] roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success();
    }
}