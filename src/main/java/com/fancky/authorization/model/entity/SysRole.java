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

    private String roleCode;

    private String roleName;

    private Integer roleSort;

    private Integer dataScope;

    private Integer status;

    private String deleteBy;

    private LocalDateTime deleteTime;

    @TableField(exist = false)
    private List<SysPermission> permissions;

    @TableField(exist = false)
    private List<Long> permissionIds;
}