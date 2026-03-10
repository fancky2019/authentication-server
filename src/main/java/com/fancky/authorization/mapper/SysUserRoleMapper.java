package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量插入用户角色关联
     */
    default int insertBatch(List<SysUserRole> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (SysUserRole item : list) {
            insert(item);
        }
        return list.size();
    }

    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询用户ID列表
     */
    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除用户的角色关联
     */
    @Select("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除角色的用户关联
     */
    @Select("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

}