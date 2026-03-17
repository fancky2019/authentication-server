package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.RoleDTO;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/role")
public class SysRoleController {

    @Autowired
    private SysRoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    public MessageResult<PageVO<SysRole>> page(RoleDTO roleDTO) {
        PageVO<SysRole> page = roleService.getRolePage(roleDTO);
        return MessageResult.success(page);
    }

    /**
     * 查询所有角色
     */
    @GetMapping("/list")
    public MessageResult<List<SysRole>> list() {
        List<SysRole> list = roleService.list();
        return MessageResult.success(list);
    }

    /**
     * 查询角色详情
     */
    @GetMapping("/{id}")
    public MessageResult<SysRole> getById(@PathVariable Long id) {
        SysRole role = roleService.getRoleWithPermissions(id);
        return MessageResult.success(role);
    }

    /**
     * 新增角色
     */
    @PostMapping("add-role")
    public MessageResult<Void> addRole(@Valid @RequestBody RoleDTO roleDTO) throws Exception {
        roleService.addRole(roleDTO);
        return MessageResult.success();
    }

    /**
     * 修改角色
     */
    @PutMapping
    public MessageResult<Void> update(@Valid @RequestBody RoleDTO roleDTO) {
        roleService.updateRole(roleDTO);
        return MessageResult.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public MessageResult<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return MessageResult.success();
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    public MessageResult<Void> deleteBatch(@RequestBody Long[] ids) {
        roleService.deleteBatch(ids);
        return MessageResult.success();
    }

    /**
     * 修改角色状态
     */
    @PutMapping("/status")
    public MessageResult<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        roleService.updateStatus(id, status);
        return MessageResult.success();
    }

    /**
     * 分配权限
     */
    @PostMapping("/assign-permissions")
    public MessageResult<Void> assignPermissions(@RequestParam Long roleId, @RequestBody Long[] permissionIds) {
        roleService.assignPermissions(roleId, permissionIds);
        return MessageResult.success();
    }
}