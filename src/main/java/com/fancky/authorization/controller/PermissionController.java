package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final SysPermissionService permissionService;

    /**
     * 查询权限列表（树形结构）
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:query')")
    public Result<List<SysPermission>> getTree() {
        List<SysPermission> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    /**
     * 查询权限详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:query')")
    public Result<SysPermission> getById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return Result.success(permission);
    }

    /**
     * 新增权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    public Result<Void> add(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.addPermission(permissionDTO);
        return Result.success();
    }

    /**
     * 修改权限
     */
    @PutMapping
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<Void> update(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(permissionDTO);
        return Result.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    /**
     * 修改权限状态
     */
    @PutMapping("/status")
    @PreAuthorize("hasAuthority('system:permission:edit')")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        permissionService.updateStatus(id, status);
        return Result.success();
    }
}