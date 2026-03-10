package com.fancky.authorization.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询部门下的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_dept WHERE dept_id = #{deptId} AND deleted = 0")
    Long countDeptUsers(@Param("deptId") Long deptId);

    /**
     * 查询子部门数量
     */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{deptId} AND deleted = 0")
    Long countChildDepts(@Param("deptId") Long deptId);

    /**
     * 查询部门下的用户ID列表
     */
    @Select("SELECT user_id FROM sys_user_dept WHERE dept_id = #{deptId} AND deleted = 0")
    List<Long> selectUserIdsByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询正常状态的部门列表
     */
    @Select("SELECT * FROM sys_dept WHERE status = 1 AND deleted = 0 ORDER BY sort ASC")
    List<SysDept> selectNormalDepts();
}
