package com.fancky.authorization.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.entity.SysUserRole;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRole> {
    void initUserRole();
    /**
     * 根据用户ID获取角色ID列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 根据角色ID获取用户ID列表
     */
    List<Long> getUserIdsByRoleId(Long roleId);

    List<SysUserRole> getByRoleId(Long roleId);
    /**
     * 为用户分配角色
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds) throws Exception;

    /**
     * 为角色分配用户
     */
    boolean assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 移除用户的某个角色
     */
    boolean removeUserRole(Long userId, Long roleId);

    /**
     * 移除用户的所有角色
     */
    boolean removeUserRoles(Long userId);

    /**
     * 移除角色的所有用户
     */
    boolean removeByRole(Long roleId) throws Exception;

    boolean deleteBatch(List<Long> idList) throws Exception;

    /**
     * 检查用户是否拥有某个角色
     */
    boolean hasRole(Long userId, Long roleId);

    /**
     * 获取用户的所有角色关联
     */
    List<SysUserRole> getUserRoles(Long userId);
}