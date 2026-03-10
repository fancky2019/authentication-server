package com.fancky.authorization.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysDept;
import com.fancky.authorization.model.entity.SysUserDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserDeptMapper extends BaseMapper<SysUserDept> {

    /**
     * 查询用户的主部门
     */
    @Select("SELECT d.* FROM sys_user_dept ud " +
            "LEFT JOIN sys_dept d ON ud.dept_id = d.id " +
            "WHERE ud.user_id = #{userId} AND ud.is_main = 1 AND ud.deleted = 0")
    SysDept selectUserMainDept(@Param("userId") Long userId);

    /**
     * 查询用户的所有部门
     */
    @Select("SELECT d.* FROM sys_user_dept ud " +
            "LEFT JOIN sys_dept d ON ud.dept_id = d.id " +
            "WHERE ud.user_id = #{userId} AND ud.deleted = 0")
    List<SysDept> selectUserDepts(@Param("userId") Long userId);

    /**
     * 批量插入用户部门关联
     */
    default int insertBatch(List<SysUserDept> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (SysUserDept item : list) {
            insert(item);
        }
        return list.size();
    }
}
