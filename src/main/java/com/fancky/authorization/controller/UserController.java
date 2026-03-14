package com.fancky.authorization.controller;

import com.fancky.authorization.model.dto.UserDTO;
import com.fancky.authorization.model.entity.SysUser;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private  SysUserService userService;

    /**
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
}