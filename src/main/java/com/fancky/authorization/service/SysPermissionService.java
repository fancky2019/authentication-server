package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.PermissionDTO;
import com.fancky.authorization.model.entity.SysPermission;


import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {
    void initPermission();
    /**
     * 获取权限树
     */
    List<SysPermission> getPermissionTree();

    /**
     * 获取用户权限
     */
    List<SysPermission> getUserPermissions(Long userId);

    SysPermission getById(Long id);

    /**
     * 获取用户权限
     */
    List<SysPermission> getPermissionByIds(List<Long> idList);

    /**
     * 新增权限
     */
    boolean addPermission(PermissionDTO permissionDTO) throws Exception;

    /**
     * 更新权限
     */
    boolean updatePermission(PermissionDTO permissionDTO) throws Exception;

    /**
     * 删除权限
     */
    boolean deletePermission(Long id) throws Exception;

    /**
     * 更新状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查是否有子节点
     */
    boolean hasChildren(Long id);
}