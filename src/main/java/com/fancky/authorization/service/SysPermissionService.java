package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;


import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 获取权限树
     */
    List<SysPermission> getPermissionTree();

    /**
     * 获取用户权限
     */
    List<SysPermission> getUserPermissions(Long userId);

    /**
     * 获取用户权限
     */
    List<SysPermission> getPermissions(List<Long> idList);

    /**
     * 新增权限
     */
    boolean addPermission(PermissionDTO permissionDTO);

    /**
     * 更新权限
     */
    boolean updatePermission(PermissionDTO permissionDTO);

    /**
     * 删除权限
     */
    boolean deletePermission(Long id);

    /**
     * 更新状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查是否有子节点
     */
    boolean hasChildren(Long id);
}