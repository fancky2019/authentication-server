package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysUserRoleMapper;
import com.fancky.authorization.model.entity.SysUserRole;
import com.fancky.authorization.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    private final SysUserRoleMapper userRoleMapper;

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        // 删除用户原有的角色关联
        userRoleMapper.deleteByUserId(userId);

        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }

        // 创建新的角色关联
        List<SysUserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        return userRoleMapper.insertBatch(userRoles) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUsersToRole(Long roleId, List<Long> userIds) {
        // 删除角色原有的用户关联
        userRoleMapper.deleteByRoleId(roleId);

        if (userIds == null || userIds.isEmpty()) {
            return true;
        }

        // 创建新的用户关联
        List<SysUserRole> userRoles = userIds.stream()
                .map(userId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        return userRoleMapper.insertBatch(userRoles) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRole(Long userId, Long roleId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRoles(Long userId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoleUsers(Long roleId) {
        return remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    public boolean hasRole(Long userId, Long roleId) {
        return count(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId)) > 0;
    }

    @Override
    public List<SysUserRole> getUserRoles(Long userId) {
        return list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }
}