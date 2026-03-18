package com.fancky.authorization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fancky.authorization.model.dto.RoleDTO;
import com.fancky.authorization.model.entity.SysRole;
import com.fancky.authorization.model.response.PageVO;

import java.util.List;


public interface SysRoleService extends IService<SysRole> {
    void initRole();
    /**
     * 分页查询角色列表
     */
    PageVO<SysRole> getRolePage(RoleDTO roleDTO);

    /**
     * 查询角色详情（包含权限）
     */
    SysRole getRoleById(Long id);

    /**
     *
     */
    SysRole getRoleByCode(String code) throws Exception;

    /**
     *
     */
    List<SysRole> getRoleByIds(List<Long> idList) throws Exception;

    /**
     * 新增角色
     */
    boolean addRole(RoleDTO roleDTO) throws Exception;

    /**
     * 更新角色
     */
    boolean updateRole(RoleDTO roleDTO) throws Exception;

    /**
     * 删除角色
     */
    boolean deleteRole(Long id) throws Exception;

    /**
     * 批量删除角色
     */
    boolean deleteBatch(Long[] ids) throws Exception;

    /**
     * 更新角色状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 分配权限
     */
    boolean assignPermissions(Long roleId, Long[] permissionIds);
}