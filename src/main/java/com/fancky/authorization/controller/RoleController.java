package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.RoleDTO;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private SysRoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    public Result<PageVO<SysRole>> page(RoleDTO roleDTO) {
        PageVO<SysRole> page = roleService.getRolePage(roleDTO);
        return Result.success(page);
    }

    /**
     * 查询所有角色
     */
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        List<SysRole> list = roleService.list();
        return Result.success(list);
    }

    /**
     * 查询角色详情
     */
    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        SysRole role = roleService.getRoleWithPermissions(id);
        return Result.success(role);
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody RoleDTO roleDTO) {
        roleService.addRole(roleDTO);
        return Result.success();
    }

    /**
     * 修改角色
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleDTO roleDTO) {
        roleService.updateRole(roleDTO);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody Long[] ids) {
        roleService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改角色状态
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        roleService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 分配权限
     */
    @PostMapping("/assign-permissions")
    public Result<Void> assignPermissions(@RequestParam Long roleId, @RequestBody Long[] permissionIds) {
        roleService.assignPermissions(roleId, permissionIds);
        return Result.success();
    }
}