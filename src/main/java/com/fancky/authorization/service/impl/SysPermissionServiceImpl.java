package com.fancky.authorization.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fancky.authorization.mapper.SysPermissionMapper;
import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSort)
        );

        return buildTree(permissions, 0L);
    }

    @Override
    public List<SysPermission> getUserPermissions(Long userId) {
        return permissionMapper.selectUserPermissions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPermission(PermissionDTO permissionDTO) {
        // 检查权限标识是否重复
        if (permissionDTO.getPermissionCode() != null) {
            Long count = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode()));
            if (count > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        SysPermission permission = new SysPermission();
        permission.setParentId(permissionDTO.getParentId() != null ? permissionDTO.getParentId() : 0L);
        permission.setPermissionName(permissionDTO.getPermissionName());
        permission.setPermissionType(permissionDTO.getPermissionType());
        permission.setPermissionCode(permissionDTO.getPermissionCode());
        permission.setPermissionValue(permissionDTO.getPermissionValue());
        permission.setPath(permissionDTO.getPath());
        permission.setComponent(permissionDTO.getComponent());
        permission.setIcon(permissionDTO.getIcon());
        permission.setSort(permissionDTO.getSort() != null ? permissionDTO.getSort() : 0);
        permission.setVisible(permissionDTO.getVisible() != null ? permissionDTO.getVisible() : 1);
        permission.setStatus(permissionDTO.getStatus() != null ? permissionDTO.getStatus() : 1);
        permission.setRemark(permissionDTO.getRemark());

        return permissionMapper.insert(permission) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermission(PermissionDTO permissionDTO) {
        SysPermission permission = permissionMapper.selectById(permissionDTO.getId());
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 检查权限标识是否重复
        if (permissionDTO.getPermissionCode() != null &&
                !permissionDTO.getPermissionCode().equals(permission.getPermissionCode())) {
            Long count = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode()));
            if (count > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        // 不能将父节点设置为自己的子节点
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId().equals(permissionDTO.getId())) {
            throw new RuntimeException("不能将父节点设置为自己");
        }

        permission.setParentId(permissionDTO.getParentId());
        permission.setPermissionName(permissionDTO.getPermissionName());
        permission.setPermissionType(permissionDTO.getPermissionType());
        permission.setPermissionCode(permissionDTO.getPermissionCode());
        permission.setPermissionValue(permissionDTO.getPermissionValue());
        permission.setPath(permissionDTO.getPath());
        permission.setComponent(permissionDTO.getComponent());
        permission.setIcon(permissionDTO.getIcon());
        permission.setSort(permissionDTO.getSort());
        permission.setVisible(permissionDTO.getVisible());
        permission.setStatus(permissionDTO.getStatus());
        permission.setRemark(permissionDTO.getRemark());

        return permissionMapper.updateById(permission) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePermission(Long id) {
        // 检查是否有子节点
        if (hasChildren(id)) {
            throw new RuntimeException("存在子权限，无法删除");
        }

        // 检查是否被角色使用
        Long count = permissionMapper.countRolePermission(id);
        if (count > 0) {
            throw new RuntimeException("该权限已被角色使用，无法删除");
        }

        return permissionMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setStatus(status);
        return permissionMapper.updateById(permission) > 0;
    }

    @Override
    public boolean hasChildren(Long id) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getParentId, id)
        );
        return count > 0;
    }

    private List<SysPermission> buildTree(List<SysPermission> permissions, Long parentId) {
        return permissions.stream()
                .filter(p -> p.getParentId().equals(parentId))
                .peek(p -> p.setChildren(buildTree(permissions, p.getId())))
                .collect(Collectors.toList());
    }
}
