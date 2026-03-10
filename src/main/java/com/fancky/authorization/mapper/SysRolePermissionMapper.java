package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量插入角色权限关联
     */
    default int insertBatch(List<SysRolePermission> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (SysRolePermission item : list) {
            insert(item);
        }
        return list.size();
    }


    /**
     * 根据角色ID查询权限ID列表
     */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID查询角色ID列表
     */
    @Select("SELECT role_id FROM sys_role_permission WHERE permission_id = #{permissionId} AND deleted = 0")
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 删除角色的权限关联
     */
    @Select("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除权限的角色关联
     */
    @Select("DELETE FROM sys_role_permission WHERE permission_id = #{permissionId}")
    void deleteByPermissionId(@Param("permissionId") Long permissionId);
}