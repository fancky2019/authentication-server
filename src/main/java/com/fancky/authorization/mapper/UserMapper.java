package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.fancky.authorization.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT u.*, " +
            "GROUP_CONCAT(DISTINCT r.role_code) as roles, " +
            "GROUP_CONCAT(DISTINCT p.permission_code) as permissions " +
            "FROM sys_user u " +
            "LEFT JOIN sys_user_role ur ON u.id = ur.user_id " +
            "LEFT JOIN sys_role r ON ur.role_id = r.id " +
            "LEFT JOIN sys_role_permission rp ON r.id = rp.role_id " +
            "LEFT JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE u.username = #{username} AND u.deleted = 0 " +
            "GROUP BY u.id")
    User selectUserWithRolesAndPermissions(@Param("username") String username);

    @Update("UPDATE sys_user SET last_login_time = #{lastLoginTime} " +
            "WHERE username = #{username} AND deleted = 0")
    int updateLastLoginTime(@Param("username") String username,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime);

    @Update("UPDATE sys_user SET password = #{password}, version = version + 1 " +
            "WHERE username = #{username} AND deleted = 0")
    int updatePassword(@Param("username") String username,
                       @Param("password") String password);

    List<User> selectUserList(@Param(Constants.WRAPPER) Wrapper<User> wrapper);

    @Update("UPDATE sys_user SET enabled = #{enabled} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close='')>" +
            "#{id}" +
            "</foreach>")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Integer enabled);
}