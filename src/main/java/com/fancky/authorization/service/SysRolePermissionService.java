package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.RolePermissionDto;
import com.fancky.authorization.model.entity.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService extends IService<SysRolePermission> {

    /**
     * 根据角色ID获取权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    List<SysRolePermission> getPermissionsByRoleIds(List<Long> roleIdList);

    /**
     * 根据权限ID获取角色ID列表
     */
    List<Long> getRoleIdsByPermissionId(Long permissionId);

    /**
     * 为角色分配权限
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 为权限分配角色
     */
    boolean addRolePermission(RolePermissionDto dto) throws Exception;

    /**
     * 移除角色的某个权限
     */
    boolean removeRolePermission(Long roleId, Long permissionId);

    /**
     * 移除角色的所有权限
     */
    boolean removeRolePermissions(Long roleId);

    /**
     * 移除权限的所有角色
     */
    boolean removePermissionRoles(Long permissionId);

    /**
     * 检查角色是否拥有某个权限
     */
    boolean hasPermission(Long roleId, Long permissionId);

    /**
     * 获取角色的所有权限关联
     */
    List<SysRolePermission> getRolePermissions(Long roleId);
}