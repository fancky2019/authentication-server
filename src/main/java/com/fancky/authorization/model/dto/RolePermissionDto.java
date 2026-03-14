package com.fancky.authorization.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionDto {
    private Long permissionId;
    private List<Long> roleIds;
}
