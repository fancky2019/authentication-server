package com.fancky.authorization.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class SysRolePermission extends BaseEntity {
    //重写 覆盖父类的    @TableLogic
//    private Integer deleted;
    @TableField("role_id")
    private Long roleId;
    @TableField("permission_id")
    private Long permissionId;
    @TableField("permission_type")
    private Integer permissionType;
}