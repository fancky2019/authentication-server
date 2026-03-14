package com.fancky.authorization.controller;

import com.fancky.authorization.model.dto.RolePermissionDto;
import com.fancky.authorization.model.response.MessageResult;
import com.fancky.authorization.service.SysRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/role-permission")
public class SysRolePermissionController {


    @Autowired
    private SysRolePermissionService rolePermissionService;


    @PostMapping("/add-role-permission")
    public MessageResult<Void> addRolePermission(@Valid @RequestBody RolePermissionDto dto) throws Exception {
        rolePermissionService.addRolePermission(dto);
        return MessageResult.success();
    }
}


