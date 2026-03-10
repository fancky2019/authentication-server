package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fancky.authorization.mapper.SysRoleMapper;
import com.fancky.authorization.mapper.SysRolePermissionMapper;
import com.fancky.authorization.model.dto.RoleDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.entity.SysRolePermission;
import com.fancky.authorization.model.response.PageVO;
import com.fancky.authorization.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Override
    public PageVO<SysRole> getRolePage(RoleDTO roleDTO) {
        Page<SysRole> page = new Page<>(roleDTO.getCurrent(), roleDTO.getSize());

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(roleDTO.getRoleCode()), SysRole::getRoleCode, roleDTO.getRoleCode())
                .like(StringUtils.hasText(roleDTO.getRoleName()), SysRole::getRoleName, roleDTO.getRoleName())
                .eq(roleDTO.getStatus() != null, SysRole::getStatus, roleDTO.getStatus())
                .orderByAsc(SysRole::getRoleSort);

        Page<SysRole> rolePage = roleMapper.selectPage(page, wrapper);
        return PageVO.build(rolePage);
    }

    @Override
    public SysRole getRoleWithPermissions(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role != null) {
            List<SysPermission> permissions = roleMapper.selectPermissionsByRoleId(id);
            role.setPermissions(permissions);
            role.setPermissionIds(permissions.stream()
                    .map(SysPermission::getId)
                    .collect(Collectors.toList()));
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRole(RoleDTO roleDTO) {
        // 检查角色编码是否存在
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleDTO.getRoleCode()));
        if (count > 0) {
            throw new RuntimeException("角色编码已存在");
        }

        // 创建角色
        SysRole role = new SysRole();
        role.setRoleCode(roleDTO.getRoleCode());
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleSort(roleDTO.getRoleSort());
        role.setDataScope(roleDTO.getDataScope() != null ? roleDTO.getDataScope() : 1);
        role.setStatus(roleDTO.getStatus() != null ? roleDTO.getStatus() : 1);
        role.setRemark(roleDTO.getRemark());

        int insert = roleMapper.insert(role);

        // 分配权限
        if (roleDTO.getPermissionIds() != null && roleDTO.getPermissionIds().length > 0) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        return insert > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(RoleDTO roleDTO) {
        SysRole role = roleMapper.selectById(roleDTO.getId());
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查角色编码是否重复
        if (!role.getRoleCode().equals(roleDTO.getRoleCode())) {
            Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleDTO.getRoleCode()));
            if (count > 0) {
                throw new RuntimeException("角色编码已存在");
            }
        }

        // 更新角色信息
        role.setRoleCode(roleDTO.getRoleCode());
        role.setRoleName(roleDTO.getRoleName());
        role.setRoleSort(roleDTO.getRoleSort());
        role.setDataScope(roleDTO.getDataScope());
        role.setStatus(roleDTO.getStatus());
        role.setRemark(roleDTO.getRemark());

        int update = roleMapper.updateById(role);

        // 更新权限
        if (roleDTO.getPermissionIds() != null) {
            // 删除原有权限
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, role.getId()));

            // 分配新权限
            if (roleDTO.getPermissionIds().length > 0) {
                assignPermissions(role.getId(), roleDTO.getPermissionIds());
            }
        }

        return update > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        // 删除角色权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));

        // 删除角色
        return roleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(Long[] ids) {
        for (Long id : ids) {
            deleteRole(id);
        }
        return true;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        return roleMapper.updateById(role) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, Long[] permissionIds) {
        // 删除原有权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));

        // 分配新权限
        List<SysRolePermission> rolePermissions = Arrays.stream(permissionIds)
                .map(permissionId -> {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permissionId);
                    return rp;
                })
                .collect(Collectors.toList());

        return rolePermissionMapper.insertBatch(rolePermissions) > 0;
    }
}