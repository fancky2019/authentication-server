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

    private Long parentId;

    private String permissionName;

    private Integer permissionType;

    private String permissionCode;

    private String permissionValue;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private Integer keepAlive;

    private String deleteBy;

    private LocalDateTime deleteTime;

    @TableField(exist = false)
    private List<SysPermission> children;
}