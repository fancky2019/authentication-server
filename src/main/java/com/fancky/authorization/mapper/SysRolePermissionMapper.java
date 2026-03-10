package com.fancky.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fancky.authorization.model.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;

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
}