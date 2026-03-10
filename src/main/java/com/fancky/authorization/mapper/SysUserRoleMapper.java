package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

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
}