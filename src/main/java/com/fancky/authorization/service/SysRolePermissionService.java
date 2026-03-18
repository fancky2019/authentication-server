package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.PermissionAssignDTO;
import com.fancky.authorization.model.entity.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService extends IService<SysRolePermission> {
    void initRolePermission();

    /**
     * 根据角色ID获取权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    List<SysRolePermission> getByRoleIds(List<Long> roleIdList);

    List<SysRolePermission> getRolePermissions();

    List<SysRolePermission> getByPermissionIds(List<Long> permissionIdList);

    boolean removeByPermissionIds(List<Long> permissionIdList) throws Exception;

    boolean deleteBatch(List<Long> idList) throws Exception;


    /**
     * 根据权限ID获取角色ID列表
     */
    List<Long> getRoleIdsByPermissionId(Long permissionId);

    /**
     * 为角色分配权限
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    void assignPermissions(PermissionAssignDTO assignDTO) throws Exception;

    /**
     * 移除角色的某个权限
     */
    boolean removeRolePermission(Long roleId, Long permissionId);

    /**
     * 移除角色的所有权限
     */
    boolean removeByRole(Long roleId) throws Exception;

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