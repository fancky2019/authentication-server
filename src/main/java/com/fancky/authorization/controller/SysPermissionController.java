package com.fancky.authorization.controller;


import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/permission")
public class SysPermissionController {

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 查询权限列表（树形结构）
     */
    @GetMapping("/tree")
    public MessageResult<List<SysPermission>> getTree() {
        List<SysPermission> tree = permissionService.getPermissionTree();
        return MessageResult.success(tree);
    }

    /**
     * 查询权限详情
     */
    @GetMapping("/{id}")
    public MessageResult<SysPermission> getById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return MessageResult.success(permission);
    }

    /**
     * 新增权限
     */
    @PostMapping("/add-permission")
    public MessageResult<Void> add(@Valid @RequestBody PermissionDTO permissionDTO) throws Exception {
        permissionService.addPermission(permissionDTO);
        return MessageResult.success();
    }

    /**
     * 修改权限
     */
    @PutMapping
    public MessageResult<Void> update(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(permissionDTO);
        return MessageResult.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    public MessageResult<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return MessageResult.success();
    }

    /**
     * 修改权限状态
     */
    @PutMapping("/status")
    public MessageResult<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        permissionService.updateStatus(id, status);
        return MessageResult.success();
    }
}