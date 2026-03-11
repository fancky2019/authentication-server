package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysRolePermissionMapper;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.service.SysRolePermissionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<SysRolePermission> getPermissionsByRoleIds(List<Long> roleIdList) {
        if (CollectionUtils.isEmpty(roleIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRolePermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SysRolePermission::getRoleId, roleIdList);
        return this.list(lambdaQueryWrapper);
    }

    @Override
    public List<Long> getRoleIdsByPermissionId(Long permissionId) {
        return rolePermissionMapper.selectRoleIdsByPermissionId(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 删除角色原有的权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        if (permissionIds == null || permissionIds.isEmpty()) {
            return true;
        }

        // 创建新的权限关联
        List<SysRolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        return rolePermissionMapper.insertBatch(rolePermissions) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToPermission(Long permissionId, List<Long> roleIds) {
        // 删除权限原有的角色关联
        rolePermissionMapper.deleteByPermissionId(permissionId);

        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }

        // 创建新的角色关联
        List<SysRolePermission> rolePermissions = roleIds.stream()
                .map(roleId -> {
                    SysRolePermission rolePermission = new SysRolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        return rolePermissionMapper.insertBatch(rolePermissions) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRolePermission(Long roleId, Long permissionId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRolePermissions(Long roleId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermissionRoles(Long permissionId) {
        return remove(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getPermissionId, permissionId));
    }

    @Override
    public boolean hasPermission(Long roleId, Long permissionId) {
        return count(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId)) > 0;
    }

    @Override
    public List<SysRolePermission> getRolePermissions(Long roleId) {
        return list(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
    }
}
