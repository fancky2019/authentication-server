package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.response.Result;
import com.fancky.authorization.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 查询权限列表（树形结构）
     */
    @GetMapping("/tree")
    public Result<List<SysPermission>> getTree() {
        List<SysPermission> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    /**
     * 查询权限详情
     */
    @GetMapping("/{id}")
    public Result<SysPermission> getById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return Result.success(permission);
    }

    /**
     * 新增权限
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.addPermission(permissionDTO);
        return Result.success();
    }

    /**
     * 修改权限
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(permissionDTO);
        return Result.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    /**
     * 修改权限状态
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        permissionService.updateStatus(id, status);
        return Result.success();
    }
}