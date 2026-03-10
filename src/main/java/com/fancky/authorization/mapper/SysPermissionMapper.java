package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户的权限列表
     */
    @Select("SELECT DISTINCT p.* FROM sys_user_role ur " +
            "LEFT JOIN sys_role_permission rp ON ur.role_id = rp.role_id " +
            "LEFT JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0 " +
            "ORDER BY p.sort ASC")
    List<SysPermission> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 查询角色使用权限的数量
     */
    @Select("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = #{permissionId}")
    Long countRolePermission(@Param("permissionId") Long permissionId);
}
