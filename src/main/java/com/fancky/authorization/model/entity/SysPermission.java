package com.fancky.authorization.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    @TableField("parent_id")
    private Long parentId;
    @TableField("permission_name")
    private String permissionName;
    @TableField("permission_type")
    private Integer permissionType;
    @TableField("permission_code")
    private String permissionCode;
    @TableField("permission_value")
    private String permissionValue;
    @TableField("path")
    private String path;
    @TableField("component")
    private String component;
    @TableField("icon")
    private String icon;
    @TableField("sort")
    private Integer sort;
    @TableField("visible")
    private Integer visible;
    @TableField("status")
    private Integer status;
    @TableField("keep_alive")
    private Integer keepAlive;



    @TableField(exist = false)
    private List<SysPermission> children;
}