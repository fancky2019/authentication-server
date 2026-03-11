package com.fancky.authorization.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @TableField("role_code")
    private String roleCode;
    @TableField("role_name")
    private String roleName;
    @TableField("role_sort")
    private Integer roleSort;
    @TableField("data_scope")
    private Integer dataScope;
    @TableField("status")
    private Integer status;


    @TableField(exist = false)
    private List<SysPermission> permissions;

    @TableField(exist = false)
    private List<Long> permissionIds;
}