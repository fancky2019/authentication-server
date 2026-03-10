package com.fancky.authorization.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysPermission;
import com.fancky.authorization.model.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询角色的权限列表
     */
    @Select("SELECT p.* FROM sys_role_permission rp " +
            "LEFT JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0")
    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}


